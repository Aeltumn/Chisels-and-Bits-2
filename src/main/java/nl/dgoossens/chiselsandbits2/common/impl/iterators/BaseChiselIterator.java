package nl.dgoossens.chiselsandbits2.common.impl.iterators;

import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import nl.dgoossens.chiselsandbits2.api.iterator.ChiselIterator;
import nl.dgoossens.chiselsandbits2.api.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;

import java.util.Optional;

/**
 * The base chisel iterator, provides implementation for getting the
 * side, integer box and bounding box.
 */
public abstract class BaseChiselIterator implements ChiselIterator {
    private Direction side;

    public BaseChiselIterator(Direction side) {
        this.side = side;
    }

    @Override
    public Direction side() {
        return side;
    }

    @Override
    public Optional<IntegerBox> getIntegerBox(final VoxelBlob blob) {
        IntegerBox box = null;
        while (hasNext()) {
            int x = x(), y = y(), z = z();
            if ((blob.getSafe(x, y, z) != VoxelBlob.AIR_BIT)) {
                if (box == null) box = new IntegerBox(15, 15, 15, 0, 0, 0);
                if(x < box.minX) box.minX = x;
                if(y < box.minY) box.minY = y;
                if(z < box.minZ) box.minZ = z;

                if(x > box.maxX) box.maxX = x;
                if(y > box.maxY) box.maxY = y;
                if(z > box.maxZ) box.maxZ = z;
            }
        }
        return Optional.ofNullable(box);
    }

    @Override
    public Optional<AxisAlignedBB> getBoundingBox(final VoxelBlob blob) {
        float oneSixteenth = 1.0f / VoxelBlob.DIMENSION;
        Optional<IntegerBox> opt = getIntegerBox(blob);
        IntegerBox box = opt.orElse(null);
        return box == null ? Optional.empty() : Optional.of(new AxisAlignedBB(box.minX * oneSixteenth, box.minY * oneSixteenth, box.minZ * oneSixteenth, (box.maxX + 1) * oneSixteenth, (box.maxY + 1) * oneSixteenth, (box.maxZ + 1) * oneSixteenth));
    }
}
