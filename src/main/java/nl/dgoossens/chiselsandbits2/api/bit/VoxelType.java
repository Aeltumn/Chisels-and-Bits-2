package nl.dgoossens.chiselsandbits2.api.bit;

/**
 * The possible types of a given bit.
 */
public enum VoxelType {
    BLOCKSTATE,
    COLOURED,
    FLUIDSTATE,
    AIR,
    ;

    /**
     * Get the voxel type that corresponds to this bit.
     */
    public static VoxelType getType(int bit) {
        //Using bitwise and and writing these in full length to ensure java doesn't screw this up. (and it's done as fast as possible, just one operation,
        // and switch statements are faster than else if)
        switch (bit & 0b11000000000000000000000000000000) {
            case 0b01000000000000000000000000000000:
                return FLUIDSTATE;
            case 0b10000000000000000000000000000000:
                return COLOURED;
            case 0b11000000000000000000000000000000:
                return BLOCKSTATE;
            default:
                return AIR;
        }
    }

    /**
     * Most efficient check to see if a bit is a fluid.
     */
    public static boolean isFluid(int bit) {
        return (bit & 0b11000000000000000000000000000000) == 0b01000000000000000000000000000000;
    }

    /**
     * Most efficient check to see if a bit is a coloured bit.
     */
    public static boolean isColoured(int bit) {
        return (bit & 0b11000000000000000000000000000000) == 0b10000000000000000000000000000000;
    }

    /**
     * Most efficient check to see if a bit is a coloured bit.
     */
    public static boolean isBlock(int bit) {
        return (bit & 0b11000000000000000000000000000000) == 0b11000000000000000000000000000000;
    }

    /**
     * Returns true if this voxel type can be collided with.
     */
    public boolean isSolid() {
        return this == BLOCKSTATE || this == COLOURED;
    }

    /**
     * Returns true if this voxel type is a fluid and can be swum in.
     */
    public boolean isFluid() {
        return this == FLUIDSTATE;
    }

    /**
     * Whether or not the other voxeltype can be seen through this
     * voxel type. (used for culling tests)
     */
    public boolean shouldShow(final VoxelType other) {
        return this != AIR && this != other;
    }
}
