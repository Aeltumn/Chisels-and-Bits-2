package nl.dgoossens.chiselsandbits2.common.impl.item;

import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;

import java.util.List;

public enum ItemModeType implements IItemModeType {
    //Type names must be identical to the startsWith() of the ItemMode!
    //Static Types
    CHISEL,
    TAPEMEASURE,
    WRENCH,
    CHISELED_BLOCK,
    ;

    @Override
    public IItemMode getDefault() {
        switch(this) {
            case CHISEL: return ItemMode.CHISEL_SINGLE;
            case TAPEMEASURE: return ItemMode.TAPEMEASURE_BIT;
            case WRENCH: return ItemMode.WRENCH_ROTATE;
            case CHISELED_BLOCK: return PlayerItemMode.CHISELED_BLOCK_FIT;
        }
        throw new UnsupportedOperationException("No default given for item mode type "+this);
    }
}
