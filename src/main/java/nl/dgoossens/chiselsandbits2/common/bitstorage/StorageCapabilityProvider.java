package nl.dgoossens.chiselsandbits2.common.bitstorage;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.common.items.StorageItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageCapabilityProvider implements ICapabilitySerializable<INBT> {
    @CapabilityInject(BitStorage.class)
    public static final Capability<BitStorage> STORAGE = null;

    private BitStorage instance;

    public StorageCapabilityProvider(final StorageItem item) {
        if(item == null) instance = new BitStorageImpl();
        else instance = new BitStorageImpl(item.getVoxelType());
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == STORAGE ? LazyOptional.of(() -> (T) this.instance) : LazyOptional.empty();
    }

    @Override
    public INBT serializeNBT() {
        return instance.toNBT();
    }

    @Override
    public void deserializeNBT(INBT nbt) {
        instance.loadFromNBT(nbt);
    }
}
