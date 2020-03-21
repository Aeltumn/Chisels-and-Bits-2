package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.*;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IItemScrollWheel;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IRotatableItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IVoxelStorer;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.NBTBlobConverter;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.MenuAction;
import nl.dgoossens.chiselsandbits2.common.network.client.CRotateItemPacket;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChiseledBlockItem extends BlockItem implements IItemScrollWheel, IItemMenu, IRotatableItem, IBitModifyItem, IVoxelStorer {
    public ChiseledBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "chiseled_block.help",
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu
        );
    }

    @Override
    public boolean canPerformModification(ModificationType type) {
        return type == ModificationType.PLACE;
    }

    @Override
    public VoxelBlob getVoxelBlob(ItemStack stack) {
        final NBTBlobConverter c = new NBTBlobConverter();
        c.readChiselData(stack.getOrCreateChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());
        return c.getVoxelBlob();
    }

    @Override
    public void setVoxelBlob(ItemStack item, VoxelBlob vb) {
        final NBTBlobConverter c = new NBTBlobConverter();
        CompoundNBT nbt = new CompoundNBT();
        c.writeChiselData(nbt);
        item.setTagInfo(ChiselUtil.NBT_BLOCKENTITYTAG, nbt);
    }

    @Override
    public void rotate(ItemStack stack, Direction.Axis axis, final boolean clockwise) {
        final NBTBlobConverter c = new NBTBlobConverter();
        c.readChiselData(stack.getOrCreateChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());

        final VoxelBlob vb = c.getVoxelBlob();
        c.setBlob(clockwise ? vb.spin(axis) : vb.spinCCW(axis));

        final CompoundNBT nbt = new CompoundNBT();
        c.writeChiselData(nbt);

        stack.setTagInfo(ChiselUtil.NBT_BLOCKENTITYTAG, nbt);
    }

    @Override
    public IItemModeType getAssociatedType() {
        return ItemModeType.CHISELED_BLOCK;
    }

    @Override
    public boolean showIconInHotbar() {
        return false;
    }

    @Override
    public Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        Set<ItemModeMenu.MenuButton> ret = new HashSet<>();
        ret.add(new ItemModeMenu.MenuButton(MenuAction.ROLL_X, -ItemModeMenu.TEXT_DISTANCE - 18, -30, Direction.WEST));
        ret.add(new ItemModeMenu.MenuButton(MenuAction.ROLL_Y, -ItemModeMenu.TEXT_DISTANCE - 18, -8, Direction.WEST));
        ret.add(new ItemModeMenu.MenuButton(MenuAction.ROLL_Z, -ItemModeMenu.TEXT_DISTANCE - 18, 14, Direction.WEST));
        return ret;
    }

    /**
     * Display the mode in the highlight tip. (and color for tape measure)
     */
    @Override
    public String getHighlightTip(ItemStack item, String displayName) {
        IItemMode im = ClientItemPropertyUtil.getChiseledBlockMode();
        return displayName + " - " + im.getLocalizedName();
    }

    @Override
    public boolean scroll(final PlayerEntity player, final ItemStack stack, final double dwheel) {
        // For now scrolling on the chiseled block rotates around the Y axis.
        // return TypedItem.scroll(player, stack, dwheel, ClientItemPropertyUtil.getGlobalCBM(), getAssociatedType());
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CRotateItemPacket(Direction.Axis.Y, dwheel >= 0));
        return true;
    }
}
