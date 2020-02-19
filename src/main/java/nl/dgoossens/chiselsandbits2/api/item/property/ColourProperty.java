package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.IntNBT;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;

public class ColourProperty extends IItemProperty<DyedItemColour> {
    private final DyedItemColour defaultColour;
    public ColourProperty(final DyedItemColour defaultColour) {
        this.defaultColour = defaultColour;
    }

    @Override
    public DyedItemColour get(ItemStack stack) {
        if(stack.hasTag() && stack.getTag().contains("colour_"+slot))
            return DyedItemColour.values()[stack.getTag().getInt("colour_"+slot)];
        return defaultColour;
    }

    @Override
    public void set(PlayerEntity player, ItemStack stack, DyedItemColour value) {
        super.set(player, stack, value);
        stack.setTagInfo("colour_"+slot, IntNBT.valueOf(value.ordinal()));
        updateStack(player, stack);
    }
}
