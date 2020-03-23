package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel;

import net.minecraft.util.math.AxisAlignedBB;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;

import java.util.Optional;

public abstract class BaseChiselIterator implements ChiselIterator {
    @Override
    public Optional<IntegerBox> getVoxelBox(final VoxelBlob blob) {
        IntegerBox box = null;
        do {
            if ((blob.get(x(), y(), z()) != VoxelBlob.AIR_BIT)) {
                if(box == null) box = new IntegerBox(16, 16, 16, 0, 0, 0);
                box.minX = Math.min(box.minX, x());
                box.minY = Math.min(box.minY, y());
                box.minZ = Math.min(box.minZ, z());
                box.maxX = Math.max(box.maxX, x());
                box.maxY = Math.max(box.maxY, y());
                box.maxZ = Math.max(box.maxZ, z());
            }
        } while (hasNext());
        return Optional.ofNullable(box);
    }

    @Override
    public Optional<AxisAlignedBB> getBoundingBox(final VoxelBlob blob) {
        final float oneSixteenth = 1.0f / VoxelBlob.DIMENSION;
        final Optional<IntegerBox> opt = getVoxelBox(blob);
        final IntegerBox box = opt.orElse(null);
        return box == null ? Optional.empty() : Optional.of(new AxisAlignedBB(box.minX * oneSixteenth, box.minY * oneSixteenth, box.minZ * oneSixteenth, (box.maxX + 1) * oneSixteenth, (box.maxY + 1) * oneSixteenth, (box.maxZ + 1) * oneSixteenth));
    }
}
