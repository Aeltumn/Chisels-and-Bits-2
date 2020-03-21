package nl.dgoossens.chiselsandbits2.common.impl.item;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class GlobalCBMCapability implements Capability.IStorage<ItemModeWrapper> {
    @Nullable
    @Override
    public INBT writeNBT(Capability<ItemModeWrapper> capability, ItemModeWrapper instance, Direction side) {
        return IntNBT.valueOf(instance.get().ordinal());
    }

    @Override
    public void readNBT(Capability<ItemModeWrapper> capability, ItemModeWrapper instance, Direction side, INBT nbt) {
        if(nbt instanceof IntNBT) {
            ItemMode a = ItemMode.values()[((IntNBT) nbt).getInt()];
            if(a.getType() == ItemModeType.CHISELED_BLOCK)
                instance.insert(a);
            else
                instance.insert(ItemModeType.CHISELED_BLOCK.getDefault());
        } else
            throw new UnsupportedOperationException("Invalid item mode.");
    }
}
