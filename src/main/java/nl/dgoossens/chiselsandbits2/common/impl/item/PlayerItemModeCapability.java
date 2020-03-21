package nl.dgoossens.chiselsandbits2.common.impl.item;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * The capability attached to a player that can be used to get
 * the set item modes that this player has selected.
 *
 * Most item modes are per-item but some are per-player, these per-player
 * modes can be retrieved from this capability.
 */
public class PlayerItemModeCapability implements Capability.IStorage<PlayerItemModeManager> {
    @Nullable
    @Override
    public INBT writeNBT(Capability<PlayerItemModeManager> capability, PlayerItemModeManager instance, Direction side) {
        return IntNBT.valueOf(instance.getChiseledBlockMode().ordinal());
    }

    @Override
    public void readNBT(Capability<PlayerItemModeManager> capability, PlayerItemModeManager instance, Direction side, INBT nbt) {
        if(nbt instanceof IntNBT) {
            PlayerItemMode a = PlayerItemMode.values()[((IntNBT) nbt).getInt()];
            if(a.getType() == ItemModeType.CHISELED_BLOCK)
                instance.setChiseledBlockMode(a);
            else
                instance.setChiseledBlockMode(ItemModeType.CHISELED_BLOCK.getDefault());
        } else
            throw new UnsupportedOperationException("Invalid chiseled block item mode.");
    }
}
