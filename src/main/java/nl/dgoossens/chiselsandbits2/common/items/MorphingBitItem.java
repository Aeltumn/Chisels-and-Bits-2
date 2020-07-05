package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.item.attributes.PropertyOwner;
import nl.dgoossens.chiselsandbits2.api.item.property.SelectedProperty;
import nl.dgoossens.chiselsandbits2.api.item.property.StateProperty;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import javax.annotation.Nullable;
import java.util.List;

public class MorphingBitItem extends SculptItem {
    protected int PROPERTY_LOCKED;
    protected int PROPERTY_SELECTED;

    public MorphingBitItem(Properties builder) {
        super(builder);

        PROPERTY_LOCKED = addProperty(new StateProperty(false));
        PROPERTY_SELECTED = addProperty(new SelectedProperty(ItemPropertyUtil::getGlobalSelectedVoxelWrapper));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "morphing_bit.help" + (isLocked(stack) ? ".locked" : ""),
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                Minecraft.getInstance().gameSettings.keyBindAttack
        );
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        if (isLocked(stack)) {
            VoxelWrapper s = getSelected(stack);
            if (s.isEmpty()) return new TranslationTextComponent(getTranslationKey() + ".locked.none");
            return new StringTextComponent(I18n.format(getTranslationKey() + ".locked").replaceAll("\\$", s.getDisplayName()));
        }
        return super.getDisplayName(stack);
    }

    public VoxelWrapper getSelected(final ItemStack stack) {
        return getProperty(PROPERTY_SELECTED, VoxelWrapper.class).get(stack);
    }

    public void setSelected(final PlayerEntity player, final ItemStack stack, final VoxelWrapper w) {
        getProperty(PROPERTY_SELECTED, VoxelWrapper.class).set(player, stack, w);
    }

    public boolean isLocked(final ItemStack stack) {
        return getProperty(PROPERTY_LOCKED, Boolean.class).get(stack);
    }

    public void setLocked(final PlayerEntity player, final ItemStack stack, final boolean value) {
        getProperty(PROPERTY_LOCKED, Boolean.class).set(player, stack, value);
    }

    //This method is called every time the creative panel is opened, so moderate performance impact if this is slow.
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            items.add(new ItemStack(this));

            //Create morphing bits for all blocks
            PropertyOwner.BUILDING_CREATIVE_TAB = true;
            final PlayerEntity player = ChiselsAndBits2.getInstance().getClient().getPlayer();
            Registry.BLOCK.forEach(b -> { //We use Registry instead of ForgeRegistries on purpose as ForgeRegistries is sorted by registry key whilst Registry is sorted by registration order. You might say: who cares? Well this way the creative menu looks more similar to the search menu.
                if (b instanceof ChiseledBlock) return; //Don't create a chiseled block morphing bit.. this will end badly.
                if (ChiselsAndBits2.getInstance().getAPI().getRestrictions().canChiselBlock(b.getDefaultState())) {
                    //Only make a locked bit if we can chisel this one
                    ItemStack it = new ItemStack(this);
                    setLocked(player, it, true);
                    setSelected(player, it, VoxelWrapper.forBlock(b));
                    items.add(it);
                }
            });
            PropertyOwner.BUILDING_CREATIVE_TAB = false;
        }
    }
}
