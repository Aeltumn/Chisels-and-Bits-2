package nl.dgoossens.chiselsandbits2.api.voxel;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The current state of a voxel tile to determine whether or not this
 * tile needs to be re-rendered.
 * Stores references to the voxel states of neighbouring tiles so it can
 * be checked whether any neighbour changed state to cause a re-render of this tile.
 */
public interface TileState {
    /**
     * Validates this render state and checks all neighbours to see if they are changed to
     * no longer be a chiseled block.
     */
    boolean validate(final World world, final BlockPos pos);

    /**
     * Returns true if dirty, calling this method
     * resets the dirty flag.
     */
    boolean isDirty();

    /**
     * Forcefully invalidates the tile state. Should be called
     * when the tile changed its voxel state.
     */
    void invalidate();
}
