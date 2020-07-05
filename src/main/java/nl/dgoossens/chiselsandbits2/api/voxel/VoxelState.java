package nl.dgoossens.chiselsandbits2.api.voxel;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.shapes.VoxelShape;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.VoxelStateImpl;

import java.util.UUID;

/**
 * A unique state of a voxel that multiple tile entities can have.
 */
public interface VoxelState {
    /**
     * The dummy state that any tile entity has when it is initially created.
     */
    public static final VoxelState NULL_STATE = new VoxelStateImpl() {
        @Override
        public boolean isDummy() {
            return true;
        }
    };

    /**
     * Returns whether or not this is a dummy state with an empty
     * blob attached.
     */
    boolean isDummy();

    /**
     * Get the unique id associated with this state.
     * IMPORTANT! State ids are not the same on both client and
     * server side! They cannot be used to communicate about voxel states.
     */
    UUID getStateId();

    /**
     * Get the voxel blob stored in this state.
     */
    VoxelBlob getBlob();

    int getPrimaryBlock();

    ItemStack getOrCreateItemStack();

    VoxelShape getSelectionShape();

    VoxelShape getCollisionShape();
}
