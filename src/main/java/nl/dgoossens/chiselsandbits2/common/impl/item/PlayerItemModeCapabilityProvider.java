package nl.dgoossens.chiselsandbits2.common.impl.item;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerItemModeCapabilityProvider implements ICapabilitySerializable<INBT> {
    @CapabilityInject(PlayerItemModeManager.class)
    public static final Capability<PlayerItemModeManager> PIMM = null;

    private PlayerItemModeManager instance = new PlayerItemModeManager();

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PIMM ? LazyOptional.of(() -> (T) this.instance) : LazyOptional.empty();
    }

    @Override
    public INBT serializeNBT() {
        return PIMM.writeNBT(instance, null);
    }

    @Override
    public void deserializeNBT(INBT nbt) {
        PIMM.readNBT(instance, null, nbt);
    }
}
