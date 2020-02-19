package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;

public class StateProperty extends IItemProperty<Boolean> {
    private final boolean defaultValue;
    public StateProperty(final boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Boolean get(final ItemStack stack) {
        if(stack.hasTag() && stack.getTag().contains("state_"+slot))
            return stack.getTag().getBoolean("state_"+slot);
        return defaultValue;
    }

    @Override
    public void set(PlayerEntity player, ItemStack stack, Boolean value) {
        super.set(player, stack, value);
        stack.setTagInfo("state_"+slot, ByteNBT.valueOf((byte) (value ? 1 : 0)));
        updateStack(player, stack);
    }
}
