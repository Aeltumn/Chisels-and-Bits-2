package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.IntNBT;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;

import java.util.function.Supplier;

public class SelectedProperty extends IItemProperty<VoxelWrapper> {
    private final Supplier<VoxelWrapper> defaultValue;
    public SelectedProperty(final Supplier<VoxelWrapper> defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public VoxelWrapper get(ItemStack stack) {
        if(stack.hasTag() && stack.getTag().contains("bit_"+slot))
            return VoxelWrapper.forAbstract(stack.getTag().getInt("bit_"+slot));
        return defaultValue.get();
    }

    @Override
    public void set(PlayerEntity player, ItemStack stack, VoxelWrapper value) {
        super.set(player, stack, value);
        stack.setTagInfo("bit_"+slot, IntNBT.valueOf(value.getId()));
        updateStack(player, stack);
    }
}
