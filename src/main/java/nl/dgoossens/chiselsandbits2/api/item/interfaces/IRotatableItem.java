package nl.dgoossens.chiselsandbits2.api.item.interfaces;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

/**
 * Represents an item that can be rotated using the ROLl_X and ROLL_Z functions.
 * Methods are called on the server-side only.
 */
public interface IRotatableItem {
    /**
     * Rotate the voxel blob stored in this item by 90 degrees on the given axis.
     */
    void rotate(final ItemStack item, final Direction.Axis axis);
}
