package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.api.render.IStateRef;

/**
 * An object storing references to all neighbouring blocks.
 */
public class ModelRenderState {
    private IStateRef north, south, east, west, up, down;
    private boolean dirty = false;

    public ModelRenderState() {
    }

    public ModelRenderState(final ModelRenderState sides) {
        if (sides != null) {
            north = sides.north;
            south = sides.south;
            east = sides.east;
            west = sides.west;
            up = sides.up;
            down = sides.down;
            dirty = true;
        }
    }

    /**
     * Returns true if dirty, calling this method
     * resets the dirty flag.
     */
    public boolean isDirty() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }

    /**
     * Forcefully invalidates the render state. Should be used
     * if the model itself changed but not its neighbours.
     */
    public void invalidate() {
        dirty = true;
    }

    public IStateRef get(final Direction side) {
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

    public void put(final Direction side, final IStateRef value) {
        if (get(side) != value) dirty = true;
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

    public boolean has(final Direction side) {
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

    public void remove(final Direction side) {
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
