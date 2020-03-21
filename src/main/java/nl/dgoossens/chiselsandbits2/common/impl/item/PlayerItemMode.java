package nl.dgoossens.chiselsandbits2.common.impl.item;

import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeEnum;

/**
 * The current mode a player can have in specific categories.
 */
public enum PlayerItemMode implements ItemModeEnum {
    CHISELED_BLOCK_GRID(12, 16), //Can't place chiseled block in spaces that already contain chiseled blocks
    CHISELED_BLOCK_FIT(9, 13), //Can only place chiseled blocks if there is no overlap with existing blocks
    CHISELED_BLOCK_OVERLAP(9, 13), //Place chiseled blocks and replace existing bits
    CHISELED_BLOCK_MERGE(9, 13), //Merge chiseled blocks and don't place bits in spots where bits already exist
    ;

    private IItemModeType type;
    private String typelessName;
    private int width, height;

    PlayerItemMode(int w, int h) {
        width = w;
        height = h;
    }

    //Cache typeless name for improved performance
    @Override
    public String getTypelessName() {
        if(typelessName == null) getType(); //force load typelessName
        return typelessName;
    }

    //We don't always calculate the type like this, we cache it.
    private IItemModeType calculateType() {
        switch(this) {
            case CHISELED_BLOCK_GRID:
            case CHISELED_BLOCK_FIT:
            case CHISELED_BLOCK_OVERLAP:
            case CHISELED_BLOCK_MERGE:
                return ItemModeType.CHISELED_BLOCK;
        }
        throw new UnsupportedOperationException("No type set for item mode "+name());
    }

    @Override
    public int getTextureWidth() {
        return width;
    }

    @Override
    public int getTextureHeight() {
        return height;
    }

    //We also cache the type.
    @Override
    public IItemModeType getType() {
        if(type == null) {
            type = calculateType();
            typelessName = name().substring(getType().name().length() + 1).toLowerCase();
        }
        return type;
    }

    @Override
    public boolean hasIcon() {
        return true;
    }

    @Override
    public boolean hasHotkey() {
        return false; //You won't be switching global item modes often so we don't do hotkeys for them.
    }
}
