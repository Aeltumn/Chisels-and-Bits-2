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

    /**
     * Get the blockstate bit id of the most common block state in this voxel.
     * If no block bits are present {@link VoxelBlob#AIR_BIT} is returned.
     */
    int getPrimaryBlock();

    /**
     * Get (or create if it has not yet been created) a chiseled block item stack
     * with the data of this voxel state in its NBT.
     */
    ItemStack getOrCreateItemStack();

    /**
     * Get the simple bounds/selection shape to use for most
     * tasks related to VoxelShapes.
     */
    VoxelShape getSelectionShape();

    /**
     * Get the complicated collision shape of this voxel to use for
     * raytracing and collision detection.
     */
    VoxelShape getCollisionShape();
}
