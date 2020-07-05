package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import net.minecraft.network.PacketBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface VoxelSerializer {
    /**
     * Get the associated voxel version.
     */
    VoxelVersions getVersion();

    /**
     * Writes all the voxelblob's data to a packet buffer.
     */
    void write(final PacketBuffer buffer, final VoxelBlob blob, final ByteArrayOutputStream o, final int best_buffer_size) throws IOException;

    /**
     * Reads this voxelblob from the supplied packet buffer.
     */
    void read(final PacketBuffer input, final VoxelBlob target);

    /**
     * Internal method to allow access to subclasses of VoxelSerializer to a voxel blob's internal int[] values.
     */
    default int[] getValues(final VoxelBlob target) {
        return target.getValues();
    }
}
