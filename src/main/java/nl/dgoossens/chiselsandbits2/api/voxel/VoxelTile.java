package nl.dgoossens.chiselsandbits2.api.voxel;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;

import java.util.Optional;

/**
 * A voxel tile represents the voxel data of a specific tile.
 */
public interface VoxelTile {
    /**
     * Returns whether or not this tile already has a
     * tile state.
     */
    boolean hasTileState();

    /**
     * Gets or creates the state of this tile. A tile doesn't need
     * a state before it is first rendered so it is possible for it not to exist.
     */
    TileState getOrCreateTileState();

    /**
     * Get the voxel state of this tile entity.
     */
    VoxelState getVoxelState();

    /**
     * Get the voxel blob stored at this tile.
     */
    VoxelBlob getVoxelBlob();

    /**
     * Get the state id of the bit at the location.
     * Safe method, impossible coordinates will be clamped to
     * be valid.
     */
    int getSafe(int x, int y, int z);

    /**
     * Get an adjacent voxel tile if there is one.
     */
    Optional<VoxelTile> getNeighbour(Direction direction);

    /**
     * Update this tile entity's voxel data to supplied data. Should be called after
     * receiving data through a packet or initialisation. If an operation happened
     * {@link #completeOperation(PlayerEntity, VoxelBlob, boolean)} should be used instead.
     */
    void updateState(VoxelBlob newState);

    /**
     * Updates the state of this tile by applying the changes to the supplied voxel blob to
     * the tile.
     */
    void completeOperation(final PlayerEntity player, final VoxelBlob vb, final boolean updateUndoTracker);
}
