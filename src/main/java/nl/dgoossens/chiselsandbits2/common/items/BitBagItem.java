package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.List;

public class BitBagItem extends StorageItem {
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "bit_bag.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if(handIn == Hand.MAIN_HAND) {
            ItemStack itemstack = playerIn.getHeldItemMainhand();
            ChiselsAndBits2.getInstance().getClient().openBitBag();
            playerIn.addStat(Stats.ITEM_USED.get(this));
            return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
        } else return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    @Override
    public VoxelType getVoxelType() {
        return VoxelType.BLOCKSTATE;
    }
}
