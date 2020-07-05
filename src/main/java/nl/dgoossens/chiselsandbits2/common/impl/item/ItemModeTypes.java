package nl.dgoossens.chiselsandbits2.common.impl.item;

import nl.dgoossens.chiselsandbits2.api.item.ItemMode;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeType;

public enum ItemModeTypes implements ItemModeType {
    //Type names must be identical to the startsWith() of the ItemModes!
    //Static Types
    CHISEL,
    TAPEMEASURE,
    WRENCH,
    CHISELED_BLOCK,
    ;

    @Override
    public ItemMode getDefault() {
        switch (this) {
            case CHISEL:
                return ItemModes.CHISEL_SINGLE;
            case TAPEMEASURE:
                return ItemModes.TAPEMEASURE_BIT;
            case WRENCH:
                return ItemModes.WRENCH_ROTATE;
            case CHISELED_BLOCK:
                return PlayerItemMode.CHISELED_BLOCK_FIT;
        }
        throw new UnsupportedOperationException("No default given for item mode type " + this);
    }
}
