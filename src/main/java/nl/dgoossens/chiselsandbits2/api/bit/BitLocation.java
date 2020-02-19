package nl.dgoossens.chiselsandbits2.api.bit;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;

public class BitLocation {
    private static final double ONE_32ND = 0.5 / VoxelBlob.DIMENSION;
    public BlockPos blockPos;
    public int bitX, bitY, bitZ;

    /**
     * Get a bit location that is being targetted from a ray trace result.
     */
    public BitLocation(BlockRayTraceResult mop, final boolean absHit, final BitOperation type) {
        final BlockPos absOffset = absHit ? mop.getPos() : BlockPos.ZERO;

        if (type != BitOperation.PLACE) {
            blockPos = mop.getPos();

            final Vec3d crds = mop.getHitVec().subtract(absOffset.getX(), absOffset.getY(), absOffset.getZ())
                    .subtract(new Vec3d(mop.getFace().getXOffset(), mop.getFace().getYOffset(), mop.getFace().getZOffset()).scale(ONE_32ND));

            bitX = snapToValid((int) Math.floor(crds.getX() * VoxelBlob.DIMENSION));
            bitY = snapToValid((int) Math.floor(crds.getY() * VoxelBlob.DIMENSION));
            bitZ = snapToValid((int) Math.floor(crds.getZ() * VoxelBlob.DIMENSION));
        } else {
            final Vec3d crds = mop.getHitVec().subtract(absOffset.getX(), absOffset.getY(), absOffset.getZ())
                    .add(new Vec3d(mop.getFace().getXOffset(), mop.getFace().getYOffset(), mop.getFace().getZOffset()).scale(ONE_32ND));

            final int bitXi = (int) Math.floor(crds.getX() * VoxelBlob.DIMENSION);
            final int bitYi = (int) Math.floor(crds.getY() * VoxelBlob.DIMENSION);
            final int bitZi = (int) Math.floor(crds.getZ() * VoxelBlob.DIMENSION);

            if (bitXi < 0 || bitYi < 0 || bitZi < 0 || bitXi >= VoxelBlob.DIMENSION || bitYi >= VoxelBlob.DIMENSION || bitZi >= VoxelBlob.DIMENSION) {
                blockPos = mop.getPos().offset(mop.getFace());
                bitX = snapToValid(bitXi - mop.getFace().getXOffset() * VoxelBlob.DIMENSION);
                bitY = snapToValid(bitYi - mop.getFace().getYOffset() * VoxelBlob.DIMENSION);
                bitZ = snapToValid(bitZi - mop.getFace().getZOffset() * VoxelBlob.DIMENSION);
            } else {
                blockPos = mop.getPos();
                bitX = snapToValid(bitXi);
                bitY = snapToValid(bitYi);
                bitZ = snapToValid(bitZi);
            }
        }
    }

    /**
     * Create a bit location from a block position and bit coordinates.
     */
    public BitLocation(final BlockPos pos, final int x, final int y, final int z) {
        blockPos = pos;
        bitX = x;
        bitY = y;
        bitZ = z;
    }

    /**
     * Get the bit location an entity is standing on top of.
     */
    public BitLocation(final Entity e) {
        blockPos = e.getPosition();
        double tx = e.getPosX() - blockPos.getX();
        double ty = e.getPosY() - blockPos.getY();
        double tz = e.getPosZ() - blockPos.getZ();
        bitX = snapToValid((int) Math.round(tx*16));
        bitY = snapToValid((int) Math.round(ty*16)) - 1;
        bitZ = snapToValid((int) Math.round(tz*16));

        if(bitY < 0) {
            blockPos = blockPos.offset(Direction.DOWN);
            bitY = 15;
        }
    }

    /**
     * Duplicate a bit location.
     */
    public BitLocation(final BitLocation other) {
        this(other.blockPos, other.bitX, other.bitY, other.bitZ);
    }

    /**
     * Get a bit location consisting of the lowest bit location in the lowest block position.
     */
    public static BitLocation min(final BitLocation from, final BitLocation to) {
        final int bitX = min(from.blockPos.getX(), to.blockPos.getX(), from.bitX, to.bitX);
        final int bitY = min(from.blockPos.getY(), to.blockPos.getY(), from.bitY, to.bitY);
        final int bitZ = min(from.blockPos.getZ(), to.blockPos.getZ(), from.bitZ, to.bitZ);

        return new BitLocation(new BlockPos(
                Math.min(from.blockPos.getX(), to.blockPos.getX()),
                Math.min(from.blockPos.getY(), to.blockPos.getY()),
                Math.min(from.blockPos.getZ(), to.blockPos.getZ())),
                bitX, bitY, bitZ);
    }

    /**
     * Get a bit location consisting of the highest bit location in the highest block position.
     */
    public static BitLocation max(final BitLocation from, final BitLocation to) {
        final int bitX = max(from.blockPos.getX(), to.blockPos.getX(), from.bitX, to.bitX);
        final int bitY = max(from.blockPos.getY(), to.blockPos.getY(), from.bitY, to.bitY);
        final int bitZ = max(from.blockPos.getZ(), to.blockPos.getZ(), from.bitZ, to.bitZ);

        return new BitLocation(new BlockPos(
                Math.max(from.blockPos.getX(), to.blockPos.getX()),
                Math.max(from.blockPos.getY(), to.blockPos.getY()),
                Math.max(from.blockPos.getZ(), to.blockPos.getZ())),
                bitX, bitY, bitZ);
    }

    private static int min(final int x, final int x2, final int bitX2, final int bitX3) {
        if (x < x2) return bitX2;
        if (x2 == x) return Math.min(bitX2, bitX3);
        return bitX3;
    }

    private static int max(final int x, final int x2, final int bitX2, final int bitX3) {
        if (x > x2) return bitX2;
        if (x2 == x) return Math.max(bitX2, bitX3);
        return bitX3;
    }

    /**
     * Add bit coordinates to this bit location.
     */
    public BitLocation add(final int x, final int y, final int z) {
        bitX += x;
        bitY += y;
        bitZ += z;
        if(bitX < 0) {
            bitX += 16;
            blockPos = blockPos.add(-1, 0, 0);
        } else if(bitX >= 16) {
            bitX -= 16;
            blockPos = blockPos.add(1, 0, 0);
        }
        if(bitY < 0) {
            bitY += 16;
            blockPos = blockPos.add(0, -1, 0);
        } else if(bitY >= 16) {
            bitY -= 16;
            blockPos = blockPos.add(0, 1, 0);
        }
        if(bitZ < 0) {
            bitZ += 16;
            blockPos = blockPos.add(0, 0, -1);
        } else if(bitZ >= 16) {
            bitZ -= 16;
            blockPos = blockPos.add(0, 0, 1);
        }
        return this;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getBitX() {
        return bitX;
    }

    public int getBitY() {
        return bitY;
    }

    public int getBitZ() {
        return bitZ;
    }

    private int snapToValid(final int x) {
        // rounding can sometimes create -1 or 16, just snap int to the nearest
        // valid position and move on.
        return Math.min(Math.max(0, x), 15);
    }

    @Override
    public String toString() {
        return "BitLocation{" +
                "blockPos=" + blockPos +
                ", bitX=" + bitX +
                ", bitY=" + bitY +
                ", bitZ=" + bitZ +
                '}';
    }
}
