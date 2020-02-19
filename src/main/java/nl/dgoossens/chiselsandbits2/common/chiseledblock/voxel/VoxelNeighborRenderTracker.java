package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;

/**
 * An object which wraps the ModelRenderState which handles
 * storing references to the states of neighbouring blocks.
 */
public final class VoxelNeighborRenderTracker {
    private final ModelRenderState sides = new ModelRenderState();

    public VoxelNeighborRenderTracker(World world, BlockPos pos) {
        update(world, pos);
    }

    /**
     * Updates the tracked neighbouring voxel references.
     */
    public void update(World world, BlockPos pos) {
        if (world == null || pos == null) return; //No point in updating if we have no world/pos.
        final TileEntity me = world.getTileEntity(pos);
        if (!(me instanceof ChiseledBlockTileEntity)) return;
        final ChiseledBlockTileEntity cme = (ChiseledBlockTileEntity) me;

        for (Direction d : Direction.values()) {
            if (sides.has(d)) continue; //Only update direction if we don't have it already

            final TileEntity te = world.getTileEntity(pos.offset(d));
            if (te instanceof ChiseledBlockTileEntity) {
                sides.put(d, ((ChiseledBlockTileEntity) te).getVoxelReference());

                //Put me in the other block to avoid circle updates
                if (((ChiseledBlockTileEntity) te).hasRenderTracker()) {
                    VoxelNeighborRenderTracker rTracker = ((ChiseledBlockTileEntity) te).getRenderTracker();
                    if (rTracker.sides.has(d.getOpposite())) continue; //If they have us, it's good
                    rTracker.sides.put(d.getOpposite(), cme.getVoxelReference());
                }
            }
        }
    }

    //Checks all neighbours and sees if any have changed to no longer be a chiseled block.
    public boolean isInvalid(final World world, final BlockPos pos) {
        if(world == null) return false; //Just in case
        final TileEntity me = world.getTileEntity(pos);
        if (me instanceof ChiseledBlockTileEntity) {
            if(this != ((ChiseledBlockTileEntity) me).getRenderTracker())
                throw new RuntimeException("Validate was called on block that was not itself.");

            for (Direction d : Direction.values()) {
                final TileEntity te = world.getTileEntity(pos.offset(d));
                if (te instanceof ChiseledBlockTileEntity)
                    sides.put(d, ((ChiseledBlockTileEntity) te).getVoxelReference());
                else
                    sides.remove(d);
            }

            //Validate the model cache right here to avoid this validation returning true time after time.
            //This also instantly invalidates the model and removes it from the cache if need be.
            return false; //TODO ChiselsAndBits2.getInstance().getClient().getRenderingManager().isInvalid(sides);
        } else throw new RuntimeException("Validate was called on block that was not even a Chiseled Block");
    }

    public void invalidate() {
        sides.invalidate();
    }

    public ModelRenderState getRenderState() {
        return sides;
    }
}
