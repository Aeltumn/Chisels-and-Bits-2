package nl.dgoossens.chiselsandbits2.common.impl.item;

import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeEnum;

/**
 * The current mode the item is using shared between all item mode types in base C&B2.
 */
public enum ItemMode implements ItemModeEnum {
    CHISEL_SINGLE(3, 3),
    CHISEL_LINE(6, 6),
    CHISEL_PLANE(9, 9),
    CHISEL_CONNECTED_PLANE(9, 9),
    CHISEL_CONNECTED_MATERIAL(9, 9),
    CHISEL_DRAWN_REGION(9, 13),
    CHISEL_SAME_MATERIAL(9, 13),
    CHISEL_SNAP8(9, 11),
    CHISEL_SNAP4(11, 13),
    CHISEL_SNAP2(13, 16),
    CHISEL_CUBE3(5, 7),
    CHISEL_CUBE5(7, 9),
    CHISEL_CUBE7(9, 13),

    TAPEMEASURE_BIT(3, 3),
    TAPEMEASURE_BLOCK(9, 13),
    TAPEMEASURE_DISTANCE(6, 6),

    WRENCH_ROTATE(10, 9),
    WRENCH_ROTATECCW(10, 9),
    WRENCH_MIRROR(12, 9),

    CHISELED_BLOCK_GRID(12, 16), //Can't place chiseled block in spaces that already contain chiseled blocks
    CHISELED_BLOCK_FIT(9, 13), //Can only place chiseled blocks if there is no overlap with existing blocks
    CHISELED_BLOCK_OVERLAP(9, 13), //Place chiseled blocks and replace existing bits
    CHISELED_BLOCK_MERGE(9, 13), //Merge chiseled blocks and don't place bits in spots where bits already exist
    ;

    private IItemModeType type;
    private String typelessName;
    private int width, height;

    ItemMode(int w, int h) {
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
            case CHISEL_SINGLE:
            case CHISEL_LINE:
            case CHISEL_PLANE:
            case CHISEL_CONNECTED_PLANE:
            case CHISEL_CONNECTED_MATERIAL:
            case CHISEL_DRAWN_REGION:
            case CHISEL_SAME_MATERIAL:
            case CHISEL_SNAP8:
            case CHISEL_SNAP4:
            case CHISEL_SNAP2:
            case CHISEL_CUBE3:
            case CHISEL_CUBE5:
            case CHISEL_CUBE7:
                return ItemModeType.CHISEL;
            case TAPEMEASURE_BIT:
            case TAPEMEASURE_BLOCK:
            case TAPEMEASURE_DISTANCE:
                return ItemModeType.TAPEMEASURE;
            case WRENCH_ROTATE:
            case WRENCH_ROTATECCW:
            case WRENCH_MIRROR:
                return ItemModeType.WRENCH;
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
        switch (this) {
            case TAPEMEASURE_BIT: //Nobody will ever use tape measure hotkeys, they just take up space in the controls menu.
            case TAPEMEASURE_BLOCK:
            case TAPEMEASURE_DISTANCE:
            case CHISELED_BLOCK_FIT: //Can't hotkey these as they are global so you won't be switching them often, they are more for preference
            case CHISELED_BLOCK_GRID:
            case CHISELED_BLOCK_MERGE:
            case CHISELED_BLOCK_OVERLAP:
                return false;
            default:
                return true;
        }
    }
}
