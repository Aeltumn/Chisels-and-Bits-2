package nl.dgoossens.chiselsandbits2.common.impl.item;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeEnum;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.*;

import static nl.dgoossens.chiselsandbits2.api.block.BitOperation.PLACE;

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
    CHISEL_SNAP8(9, 11), //0.125 size (1/8)
    CHISEL_SNAP4(11, 13), //0.25 size (1/4)
    CHISEL_SNAP2(13, 16), // 0.5 size (1/2)
    CHISEL_CUBE3(5, 7),
    CHISEL_CUBE5(7, 9),
    CHISEL_CUBE7(9, 13),

    TAPEMEASURE_BIT(3, 3),
    TAPEMEASURE_BLOCK(9, 13),
    TAPEMEASURE_DISTANCE(6, 6),

    WRENCH_ROTATE(10, 9),
    WRENCH_ROTATECCW(10, 9),
    WRENCH_MIRROR(12, 9),
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
                return false;
            default:
                return true;
        }
    }

    @Override
    public ChiselIterator getIterator(BlockPos bitPosition, Direction side, BitOperation place, IVoxelSrc source) {
        if (this == ItemMode.CHISEL_CONNECTED_MATERIAL)
            return new ChiselExtrudeMaterialIterator(bitPosition, source, side, place.equals(PLACE));

        if (this == ItemMode.CHISEL_CONNECTED_PLANE)
            return new ChiselExtrudeIterator(bitPosition, source, side, place.equals(PLACE));

        if (this == ItemMode.CHISEL_SAME_MATERIAL)
            return new ChiselMaterialIterator(bitPosition, source, side, place.equals(PLACE));

        return new ChiselTypeIterator(this, bitPosition, side);
    }

    @Override
    public ChiselIterator getIterator(final BlockPos bitPosition, final Direction side, final BitOperation place, final IVoxelSrc source, final BitLocation from, final BitLocation to) {
        if (this == ItemMode.CHISEL_DRAWN_REGION) {
            final int bitX = bitPosition.getX() == from.blockPos.getX() ? from.bitX : 0;
            final int bitY = bitPosition.getY() == from.blockPos.getY() ? from.bitY : 0;
            final int bitZ = bitPosition.getZ() == from.blockPos.getZ() ? from.bitZ : 0;

            final int scaleX = (bitPosition.getX() == to.blockPos.getX() ? to.bitX : 15) - bitX + 1;
            final int scaleY = (bitPosition.getY() == to.blockPos.getY() ? to.bitY : 15) - bitY + 1;
            final int scaleZ = (bitPosition.getZ() == to.blockPos.getZ() ? to.bitZ : 15) - bitZ + 1;
            return new ChiselTypeIterator(bitX, bitY, bitZ, scaleX, scaleY, scaleZ, side);
        }
        return getIterator(bitPosition, side, place, source);
    }
}
