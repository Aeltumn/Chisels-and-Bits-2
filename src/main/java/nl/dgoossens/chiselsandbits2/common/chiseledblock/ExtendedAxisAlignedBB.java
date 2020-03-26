package nl.dgoossens.chiselsandbits2.common.chiseledblock;

import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A variant of the axis aligned bounding box with support to check
 * if it is oversized.
 */
public class ExtendedAxisAlignedBB extends AxisAlignedBB {
    public ExtendedAxisAlignedBB(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Snaps this bounding box to the nearest block.
     */
    public ExtendedAxisAlignedBB snapToBlocks() {
        return new ExtendedAxisAlignedBB(Math.floor(minX), Math.floor(minY), Math.floor(minZ), Math.ceil(maxX), Math.ceil(maxY), Math.ceil(maxZ));
    }

    /**
     * Check if the bounding box has no difference in any direction greater than the input.
     */
    public boolean isSmallerThan(double maxSize) {
        maxSize += 0.01; //To prevent floating point issues.
        return maxX - minX <= maxSize && maxY - minY <= maxSize && maxZ - minZ <= maxSize;
    }

    /**
     * Check if the bounding box has no difference in any direction smaller than the input.
     */
    public boolean isLargerThan(double maxSize) {
        maxSize += 0.01; //To prevent floating point issues.
        return maxX - minX > maxSize || maxY - minY > maxSize || maxZ - minZ > maxSize;
    }

    /**
     * Get the length of an axis of this bounding box.
     */
    public double getLength(Direction.Axis axis) {
        switch(axis) {
            case X: return maxX - minX;
            case Y: return maxY - minY;
            case Z: return maxZ - maxZ;
        }
        return 0;
    }
}
