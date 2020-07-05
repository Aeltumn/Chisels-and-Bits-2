package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.IntNBT;
import nl.dgoossens.chiselsandbits2.api.item.ItemMode;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModes;

public class ItemModeProperty extends ItemProperty<ItemMode> {
    private final ItemModeType type;

    public ItemModeProperty(final ItemModeType type) {
        this.type = type;
    }

    @Override
    public ItemMode get(final ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("bmode_" + slot)) {
            boolean b = stack.getTag().getBoolean("bmode_" + slot);
            int i = stack.getTag().getInt("mode_" + slot);
            if (b) {
                return i >= ItemModes.values().length ? type.getDefault() : ItemModes.values()[i];
            } else
                throw new UnsupportedOperationException("No support for custom item mode properties yet!");
        }
        return type.getDefault();
    }

    @Override
    public void set(PlayerEntity player, ItemStack stack, ItemMode value) {
        super.set(player, stack, value);
        if (value instanceof ItemModes) {
            stack.setTagInfo("bmode_" + slot, ByteNBT.valueOf((byte) 1));
            stack.setTagInfo("mode_" + slot, IntNBT.valueOf(((ItemModes) value).ordinal()));
        } else
            stack.setTagInfo("bmode_" + slot, ByteNBT.valueOf((byte) 0));
        updateStack(player, stack);
    }
}
