package nl.dgoossens.chiselsandbits2.common.bitstorage;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;
import nl.dgoossens.chiselsandbits2.api.BagStorage;

import javax.annotation.Nullable;

public class BagCapability implements Capability.IStorage<BagStorage> {
    @Nullable
    @Override
    public INBT writeNBT(Capability<BagStorage> capability, BagStorage instance, Direction side) {
        CompoundNBT compound = new CompoundNBT();
        if(instance instanceof BagStorageImpl)
            ((BagStorageImpl) instance).content.forEach((k, v) ->
                    compound.putLong(k.getRegistryName().toString(), v));
        return compound;
    }

    @Override
    public void readNBT(Capability<BagStorage> capability, BagStorage instance, Direction side, INBT nbt) {
        if(nbt instanceof CompoundNBT) {
            ((CompoundNBT) nbt).keySet().forEach(k ->
                    instance.setAmount(ForgeRegistries.BLOCKS.getValue(ResourceLocation.create(k, ':')), ((CompoundNBT) nbt).getLong(k)));
        }
    }
}
