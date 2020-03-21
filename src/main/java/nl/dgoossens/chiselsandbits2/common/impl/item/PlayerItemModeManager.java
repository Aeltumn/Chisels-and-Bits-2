package nl.dgoossens.chiselsandbits2.common.impl.item;

import nl.dgoossens.chiselsandbits2.api.item.IItemMode;

/**
 * The manager of the various per-player item modes that a given
 * player has selected.
 */
public class PlayerItemModeManager {
    private PlayerItemMode chiseledBlockMode = (PlayerItemMode) ItemModeType.CHISELED_BLOCK.getDefault();

    public PlayerItemMode getChiseledBlockMode() {
        return chiseledBlockMode;
    }

    public void setChiseledBlockMode(IItemMode mode) {
        if(mode instanceof PlayerItemMode)
            chiseledBlockMode = (PlayerItemMode) mode;
    }
}
