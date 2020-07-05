package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import com.google.common.base.Preconditions;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob.DIMENSION;

/**
 * Represents multiple voxel blobs and allows for shifting voxel blobs and easily splitting it into
 * multiple parts.
 */
public class ExtendedVoxelBlob {
    private final int xOffset, yOffset, zOffset;
    private final int width, height, depth;
    private VoxelBlob[][][] subVoxels;

    public ExtendedVoxelBlob(int width, int height, int depth, int xOffset, int yOffset, int zOffset) {
        subVoxels = new VoxelBlob[width][height][depth];
        this.xOffset = -xOffset;
        this.yOffset = -yOffset;
        this.zOffset = -zOffset;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    //Internal method for easily validating coordinates.
    private void validateCoordinates(int x, int y, int z) {
        Preconditions.checkArgument(x + xOffset >= 0 && x + xOffset < width, "X coordinate out of bounds");
        Preconditions.checkArgument(y + yOffset >= 0 && y + yOffset < height, "Y coordinate out of bounds");
        Preconditions.checkArgument(z + zOffset >= 0 && z + zOffset < depth, "Z coordinate out of bounds");
    }

    /**
     * Inserts a voxel blob into the extended blob.
     */
    public void insertBlob(int x, int y, int z, VoxelBlob blob) {
        validateCoordinates(x, y, z);
        subVoxels[x + xOffset][y + yOffset][z + zOffset] = blob;
    }

    /**
     * Shift the bits inside all voxelblobs by an amount of bits.
     */
    public void shift(int xBits, int yBits, int zBits) {
        VoxelBlob[][][] newVoxels = new VoxelBlob[width][height][depth];
        for (int bx = 0; bx < width; bx++) {
            for (int by = 0; by < height; by++) {
                for (int bz = 0; bz < depth; bz++) {
                    final VoxelBlob b = subVoxels[bx][by][bz];
                    if (b == null) continue; //Can't shift empty voxels.

                    for (int z = 0; z < DIMENSION; z++) {
                        for (int y = 0; y < DIMENSION; y++) {
                            for (int x = 0; x < DIMENSION; x++) {
                                if (b.get(x, y, z) == VoxelBlob.AIR_BIT) continue; //No use in shifting air.

                                int xx = x + xBits;
                                int yy = y + yBits;
                                int zz = z + zBits;
                                int blobX = xOffset, blobY = yOffset, blobZ = zOffset;
                                if (xx < 0) {
                                    xx += DIMENSION;
                                    blobX -= 1;
                                } else if (xx >= DIMENSION) {
                                    xx -= DIMENSION;
                                    blobX += 1;
                                }
                                if (yy < 0) {
                                    yy += DIMENSION;
                                    blobY -= 1;
                                } else if (yy >= DIMENSION) {
                                    yy -= DIMENSION;
                                    blobY += 1;
                                }
                                if (zz < 0) {
                                    zz += DIMENSION;
                                    blobZ -= 1;
                                } else if (zz >= DIMENSION) {
                                    zz -= DIMENSION;
                                    blobZ += 1;
                                }
                                VoxelBlob target = newVoxels[blobX][blobY][blobZ];
                                if (target == null) target = VoxelBlob.getAirBlob();
                                target.set(xx, yy, zz, b.get(x, y, z));
                                newVoxels[blobX][blobY][blobZ] = target;
                            }
                        }
                    }
                }
            }
        }

        //Set subvoxels to new values
        subVoxels = new VoxelBlob[width][height][depth]; //Clear subvoxels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    final VoxelBlob b = subVoxels[x][y][z];
                    if (b == null)
                        subVoxels[x][y][z] = newVoxels[x][y][z];
                    else
                        subVoxels[x][y][z] = b.merge(newVoxels[x][y][z]);
                }
            }
        }
    }

    /**
     * Get the voxel blob at a certain position.
     */
    public VoxelBlob getSubVoxel(int x, int y, int z) {
        validateCoordinates(x, y, z);
        return subVoxels[x + xOffset][y + yOffset][z + zOffset];
    }

    /**
     * Return a collection of all relative block positions in this extended blob.
     * Ignores positions containing zero bits.
     */
    public Collection<BlockPos> listBlocks() {
        Set<BlockPos> ret = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (subVoxels[x][y][z] != null)
                        ret.add(new BlockPos(x - xOffset, y - yOffset, z - zOffset));
                }
            }
        }
        return ret;
    }
}
