package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.IntNBT;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;

public class ItemModeProperty extends IItemProperty<IItemMode> {
    private  final IItemModeType type;
    public ItemModeProperty(final IItemModeType type) {
        this.type = type;
    }

    @Override
    public IItemMode get(final ItemStack stack) {
        if(stack.hasTag() && stack.getTag().contains("bmode_"+slot)) {
            boolean b = stack.getTag().getBoolean("bmode_"+slot);
            if(b) {
                return ItemMode.values()[stack.getTag().getInt("mode_"+slot)];
            } else
                throw new UnsupportedOperationException("No support for custom item mode properties yet!");
        }
        return type.getDefault();
    }

    @Override
    public void set(PlayerEntity player, ItemStack stack, IItemMode value) {
        super.set(player, stack, value);
        if(value instanceof ItemMode) {
            stack.setTagInfo("bmode_"+slot, ByteNBT.valueOf((byte) 1));
            stack.setTagInfo("mode_"+slot, IntNBT.valueOf(((ItemMode) value).ordinal()));
        } else
            stack.setTagInfo("bmode_"+slot, ByteNBT.valueOf((byte) 0));
        updateStack(player, stack);
    }
}
