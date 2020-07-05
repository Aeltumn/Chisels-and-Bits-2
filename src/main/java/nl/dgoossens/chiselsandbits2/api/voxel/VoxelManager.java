package nl.dgoossens.chiselsandbits2.api.voxel;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * The voxel manager manages which unique types of voxel states exist in the world and attempts to optimise
 * this by reusing data for multiple chiseled blocks where possible.
 */
public interface VoxelManager {
    /**
     * Get the voxel tile at a given block.
     */
    Optional<VoxelTile> getVoxelTile(final World world, final BlockPos pos);

    /**
     * Get the voxel state for a given state id.
     */
    Optional<VoxelState> getVoxelState(final UUID stateId);

    /**
     * Submit a new voxel blob to be turned into a state. Returns the id
     * attached to this newly created state.
     */
    UUID submitNewVoxelState(VoxelBlob voxelBlob);
}
