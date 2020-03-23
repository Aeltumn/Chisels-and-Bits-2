package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel;

import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;

import java.util.*;

public class ChiselExtrudeIterator extends BaseChiselIterator implements ChiselIterator {
    final int INDEX_X = 0;
    final int INDEX_Y = 8;
    final int INDEX_Z = 16;
    // future state.
    Iterator<Integer> list;
    // present state.
    Direction side;
    int value;

    public ChiselExtrudeIterator(
            final int dim,
            final int sx,
            final int sy,
            final int sz,
            final IVoxelSrc source,
            final IItemMode mode,
            final Direction side,
            final boolean place) {
        this.side = side;

        final Set<Integer> possiblepositions = new HashSet<Integer>();
        final List<Integer> selectedpositions = new ArrayList<Integer>();

        final int tx = side.getXOffset(), ty = side.getYOffset(), tz = side.getZOffset();
        int placeoffset = 0;

        int x = sx, y = sy, z = sz;

        if (place) {
            x -= tx;
            y -= ty;
            z -= tz;
            placeoffset = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        }

        readyMatching(source, x, y, z);

        for (int b = 0; b < dim; ++b) {
            for (int a = 0; a < dim; ++a) {
                switch (side) {
                    case DOWN:
                    case UP:
                        if (isMatch(source, a, y, b) && source.getSafe(a + tx, y + ty, b + tz) == 0) {
                            possiblepositions.add(createPos(a, y + placeoffset, b));
                        }
                        break;
                    case EAST:
                    case WEST:
                        if (isMatch(source, x, a, b) && source.getSafe(x + tx, a + ty, b + tz) == 0) {
                            possiblepositions.add(createPos(x + placeoffset, a, b));
                        }
                        break;
                    case NORTH:
                    case SOUTH:
                        if (isMatch(source, a, b, z) && source.getSafe(a + tx, b + ty, z + tz) == 0) {
                            possiblepositions.add(createPos(a, b, z + placeoffset));
                        }
                        break;
                    default:
                        throw new NullPointerException();
                }
            }
        }

        floodFill(sx, sy, sz, possiblepositions, selectedpositions);
        Collections.sort(selectedpositions, (a, b) -> {
            final int aX = getValue(a, INDEX_X);
            final int bX = getValue(b, INDEX_X);
            if (aX != bX) {
                return aX - bX;
            }

            final int aY = getValue(a, INDEX_Y);
            final int bY = getValue(b, INDEX_Y);
            if (aY != bY) {
                return aY - bY;
            }

            final int aZ = getValue(a, INDEX_Z);
            final int bZ = getValue(b, INDEX_Z);
            return aZ - bZ;
        });

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

    protected void readyMatching(
            final IVoxelSrc source,
            final int x,
            final int y,
            final int z) {

    }

    protected boolean isMatch(
            final IVoxelSrc source,
            final int x,
            final int y,
            final int z) {
        return source.getSafe(x, y, z) != 0;
    }

    private void floodFill(
            final int sx,
            final int sy,
            final int sz,
            final Set<Integer> possiblepositions,
            final List<Integer> selectedpositions) {
        final Queue<Integer> q = new LinkedList<Integer>();
        q.add(createPos(sx, sy, sz));

        while (!q.isEmpty()) {
            final int pos = q.poll();
            selectedpositions.add(pos);

            final int x = getValue(pos, INDEX_X);
            final int y = getValue(pos, INDEX_Y);
            final int z = getValue(pos, INDEX_Z);

            possiblepositions.remove(pos);

            addIfExists(q, possiblepositions, createPos(x - 1, y, z));
            addIfExists(q, possiblepositions, createPos(x + 1, y, z));
            addIfExists(q, possiblepositions, createPos(x, y - 1, z));
            addIfExists(q, possiblepositions, createPos(x, y + 1, z));
            addIfExists(q, possiblepositions, createPos(x, y, z - 1));
            addIfExists(q, possiblepositions, createPos(x, y, z + 1));
        }
    }

    private void addIfExists(
            final Queue<Integer> q,
            final Set<Integer> possiblepositions,
            final int pos) {
        if (possiblepositions.contains(pos)) {
            possiblepositions.remove(pos);
            q.add(pos);
        }
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

    public static class ChiselExtrudeMaterialIterator extends ChiselExtrudeIterator {

        int material = 0;

        public ChiselExtrudeMaterialIterator(
                final int dim,
                final int sx,
                final int sy,
                final int sz,
                final IVoxelSrc source,
                final IItemMode mode,
                final Direction side,
                final boolean place) {
            super(dim, sx, sy, sz, source, mode, side, place);
        }

        @Override
        protected void readyMatching(
                final IVoxelSrc source,
                final int x,
                final int y,
                final int z) {
            material = source.getSafe(x, y, z);
        }

        @Override
        protected boolean isMatch(
                final IVoxelSrc source,
                final int x,
                final int y,
                final int z) {
            return source.getSafe(x, y, z) == material;
        }

    }

}
