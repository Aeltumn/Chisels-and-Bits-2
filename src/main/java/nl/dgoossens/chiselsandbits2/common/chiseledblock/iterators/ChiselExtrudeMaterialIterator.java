package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;

public class ChiselExtrudeMaterialIterator extends ChiselExtrudeIterator {
    private int material = 0;

    public ChiselExtrudeMaterialIterator(final BlockPos pos, final IVoxelSrc source, final Direction side, final boolean place) {
        super(pos, source, side, place);
    }

    @Override
    protected void readyMatching(final IVoxelSrc source, final int x, final int y, final int z) {
        material = source.getSafe(x, y, z);
    }

    @Override
    protected boolean isMatch(final IVoxelSrc source, final int x, final int y, final int z) {
        return source.getSafe(x, y, z) == material;
    }
}
