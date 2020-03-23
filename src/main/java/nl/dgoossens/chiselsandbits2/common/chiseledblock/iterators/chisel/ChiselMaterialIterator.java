package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel;

import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.bit.BitIterator;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChiselMaterialIterator extends BaseChiselIterator implements ChiselIterator {
    final int INDEX_X = 0;
    final int INDEX_Y = 8;
    final int INDEX_Z = 16;

    // future state.
    Iterator<Integer> list;

    // present state.
    Direction side;
    int value;

    public ChiselMaterialIterator(
            final int dim,
            final int sx,
            final int sy,
            final int sz,
            final IVoxelSrc source,
            final IItemMode mode,
            final Direction side,
            final boolean place) {
        this.side = side;
        final List<Integer> selectedpositions = new ArrayList<Integer>();

        final int tx = side.getXOffset(), ty = side.getYOffset(), tz = side.getZOffset();

        int x = sx, y = sy, z = sz;

        int placeoffsetX = 0;
        int placeoffsetY = 0;
        int placeoffsetZ = 0;

        if (place) {
            x -= tx;
            y -= ty;
            z -= tz;
            placeoffsetX = side.getAxis() == Axis.X ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
            placeoffsetY = side.getAxis() == Axis.Y ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
            placeoffsetZ = side.getAxis() == Axis.Z ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
        }

        final int target = source.getSafe(x, y, z);

        final BitIterator bi = new BitIterator();
        while (bi.hasNext()) {
            int xx = -1, yy = -1, zz = -1;
            if (source.getSafe(bi.x, bi.y, bi.z) == target) {
                xx = placeoffsetX + bi.x;
                yy = placeoffsetY + bi.y;
                zz = placeoffsetZ + bi.z;
            } else if (source.getSafe(bi.x - tx, bi.y - ty, bi.z - tz) == target) {
                xx = placeoffsetX + bi.x - tx;
                yy = placeoffsetY + bi.y - ty;
                zz = placeoffsetZ + bi.z - tz;
            }

            if (xx >= 0 && xx < VoxelBlob.DIMENSION &&
                    yy >= 0 && yy < VoxelBlob.DIMENSION &&
                    zz >= 0 && zz < VoxelBlob.DIMENSION)
                selectedpositions.add(createPos(xx, yy, zz));
        }

        // we are done, drop the list and keep an iterator.
        list = selectedpositions.iterator();
    }

    private int setValue(
            final int pos,
            final int idx) {
        return ((byte) pos & 0xff) << idx;
    }

    private int getValue(
            final int value,
            final int idx) {
        return (byte) (value >>> idx & 0xff);
    }

    private int createPos(
            final int x,
            final int y,
            final int z) {
        return setValue(x, INDEX_X) | setValue(y, INDEX_Y) | setValue(z, INDEX_Z);
    }

    @Override
    public boolean hasNext() {
        if (list.hasNext()) {
            value = list.next();
            return true;
        }

        return false;
    }

    @Override
    public Direction side() {
        return side;
    }

    @Override
    public int x() {
        return getValue(value, INDEX_X);
    }

    @Override
    public int y() {
        return getValue(value, INDEX_Y);
    }

    @Override
    public int z() {
        return getValue(value, INDEX_Z);
    }
}
