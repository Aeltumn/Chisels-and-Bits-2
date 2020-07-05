package nl.dgoossens.chiselsandbits2.common.impl.voxel;

import com.google.common.base.Preconditions;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.api.voxel.TileState;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;

import java.util.UUID;

public final class TileStateImpl implements TileState {
    private UUID north, south, east, west, up, down;
    private boolean dirty = false;

    public TileStateImpl(World world, BlockPos pos) {
        if (world == null || pos == null) return; //No point in updating if we have no world/pos.
        final TileEntity me = world.getTileEntity(pos);
        if (!(me instanceof ChiseledBlockTileEntity)) return;
        final ChiseledBlockTileEntity cme = (ChiseledBlockTileEntity) me;

        for (Direction d : Direction.values()) {
            if (has(d)) continue; //Only update direction if we don't have it already

            final TileEntity neighbour = world.getTileEntity(pos.offset(d));
            if (neighbour instanceof ChiseledBlockTileEntity) {
                //Store a reference to this neighbour
                put(d, ((ChiseledBlockTileEntity) neighbour).getVoxelState().getStateId());

                //Put the state id of this block into the neighbour's render state
                //for convenience, but only if it already has a render state.
                if (((ChiseledBlockTileEntity) neighbour).hasTileState()) {
                    TileStateImpl neighbourState = (TileStateImpl) ((ChiseledBlockTileEntity) neighbour).getOrCreateTileState();
                    neighbourState.put(d.getOpposite(), cme.getVoxelState().getStateId());
                }
            }
        }
    }

    @Override
    public boolean validate(final World world, final BlockPos pos) {
        if (world == null) return false; //Cancel just in case the world is null.

        final TileEntity me = world.getTileEntity(pos);
        if (me instanceof ChiseledBlockTileEntity) {
            if (!((ChiseledBlockTileEntity) me).hasTileState() || this != ((ChiseledBlockTileEntity) me).getOrCreateTileState())
                throw new RuntimeException("A tile state was requested to be validated on the wrong block");

            //Update all state ids because we are re-rendering anyways
            for (Direction d : Direction.values()) {
                final TileEntity te = world.getTileEntity(pos.offset(d));
                if (te instanceof ChiseledBlockTileEntity)
                    put(d, ((ChiseledBlockTileEntity) te).getVoxelState().getStateId());
                else
                    remove(d);
            }

            //Validate the model cache right here to avoid this validation returning true time after time.
            //This also instantly invalidates the model and removes it from the cache if need be.
            return false; //TODO ChiselsAndBits2.getInstance().getClient().getRenderingManager().isInvalid(this);
        } else throw new RuntimeException("Validate was called on block that was not even a Chiseled Block");
    }

    @Override
    public boolean isDirty() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }

    @Override
    public void invalidate() {
        dirty = true;
    }

    private UUID get(final Direction side) {
        switch (side) {
            case DOWN:
                return down;
            case EAST:
                return east;
            case NORTH:
                return north;
            case SOUTH:
                return south;
            case UP:
                return up;
            case WEST:
                return west;
            default:
                return null;
        }
    }

    private void put(final Direction side, final UUID value) {
        Preconditions.checkArgument(value != null, "Can't put a null value into a tile state, use remove instead!");
        //If the current value is not equal to the new value we mark this tile state as in need of a re-render.
        if (!value.equals(get(side))) dirty = true;
        switch (side) {
            case DOWN:
                down = value;
                break;
            case EAST:
                east = value;
                break;
            case NORTH:
                north = value;
                break;
            case SOUTH:
                south = value;
                break;
            case UP:
                up = value;
                break;
            case WEST:
                west = value;
                break;
        }
    }

    private boolean has(final Direction side) {
        switch (side) {
            case DOWN:
                return down != null;
            case EAST:
                return east != null;
            case NORTH:
                return north != null;
            case SOUTH:
                return south != null;
            case UP:
                return up != null;
            case WEST:
                return west != null;
        }
        return false;
    }

    private void remove(final Direction side) {
        if (has(side)) dirty = true;
        switch (side) {
            case DOWN:
                down = null;
                break;
            case EAST:
                east = null;
                break;
            case NORTH:
                north = null;
                break;
            case SOUTH:
                south = null;
                break;
            case UP:
                up = null;
                break;
            case WEST:
                west = null;
                break;
        }
    }
}
