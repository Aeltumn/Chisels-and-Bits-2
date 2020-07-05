package nl.dgoossens.chiselsandbits2.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;

public class UndoStep {
    private final DimensionType dimension;
    private final BlockPos pos;
    private final VoxelBlob before, after;
    private UndoStep next; //Groups form a linked chain.

    public UndoStep(World world, BlockPos pos, VoxelBlob before, VoxelBlob after) {
        dimension = world.dimension.getType();
        this.pos = pos;
        this.before = before;
        this.after = after;
    }

    /**
     * Chains the other undo step onto this one.
     */
    public void chain(UndoStep step) {
        next = step;
    }

    /**
     * Get the chained undo step.
     */
    public UndoStep getChained() {
        return next;
    }

    /**
     * Get the block's position.
     */
    public BlockPos getPosition() {
        return pos;
    }

    /**
     * Get the before state.
     */
    public VoxelBlob getBefore() {
        return before;
    }

    /**
     * Get the after state.
     */
    public VoxelBlob getAfter() {
        return after;
    }

    /**
     * Checks if this step is correct for this player.
     */
    public boolean isCorrect(PlayerEntity player) {
        return after != null && before != null && player.dimension.equals(dimension);
    }
}
