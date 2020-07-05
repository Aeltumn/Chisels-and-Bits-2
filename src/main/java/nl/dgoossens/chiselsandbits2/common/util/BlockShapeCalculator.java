package nl.dgoossens.chiselsandbits2.common.util;

import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Calculates the block shape of a VoxelBlob.
 */
public class BlockShapeCalculator {
    /**
     * Calculates both the selection shape and the collision shape for a voxel blob.
     */
    public static Pair<VoxelShape, VoxelShape> calculate(final VoxelBlob blob) {
        VoxelShape collisionShape = VoxelShapes.empty();
        int x1 = 15, y1 = 15, z1 = 15, x2 = 0, y2 = 0, z2 = 0;

        // For every x/z coordinate we build shapes from the bottom up
        double bitDimension = 1 / 16.0d;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double dx = x / 16.0d;
                double dz = z / 16.0d;

                int start = -1;
                for (int y = 0; y < 16; y++) {
                    // If this bit isn't air we start the shape here
                    if (blob.get(x, y, z) != VoxelBlob.AIR_BIT) {
                        if (start == -1) {
                            start = y;
                            if (y < y1) y1 = y;
                        }
                    } else if (start != -1) {
                        //If this is air and we were working on a box we end it
                        collisionShape = VoxelShapes.or(collisionShape, VoxelShapes.create(dx, start / 16.0d, dz, dx + bitDimension, (y - 1) / 16.0d, dz + bitDimension));
                        start = -1;
                        if (x < x1) x1 = x;
                        if (z < z1) z1 = z;
                        if (x > x2) x2 = x;
                        if (y > y2) y2 = y - 1;
                        if (z > z2) z2 = z;
                    }
                }
                if (start != -1) {
                    //If we ended with a box we add that too
                    collisionShape = VoxelShapes.or(collisionShape, VoxelShapes.create(dx, start / 16.0d, dz, dx + bitDimension, 1.0d, dz + bitDimension));
                    y2 = 15;
                }
            }
        }

        // Determine selection shape by taking the bounds
        boolean invalid = (x1 > x2) || (y1 > y2) || (z1 > z2);
        VoxelShape selectionShape = invalid ? VoxelShapes.empty() : VoxelShapes.create(x1 / 16.0d, y1 / 16.0d, z1 / 16.0d, x2 / 16.0d, y2 / 16.0d, z2 / 16.0d);
        return Pair.of(selectionShape, collisionShape);
    }
}
