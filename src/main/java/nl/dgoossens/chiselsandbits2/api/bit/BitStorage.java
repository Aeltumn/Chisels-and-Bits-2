package nl.dgoossens.chiselsandbits2.api.bit;

import net.minecraft.nbt.INBT;

/**
 * Represents an interface capable of storing bits of any type.
 * Any methods that modify the bit storage may only be ran on the client, it is read-only
 * on the client-side.
 */
public interface BitStorage {
    /**
     * Returns how many slots this storage has.
     */
    int getMaximumSlots();

    /**
     * Finds the first available slot for a given type.
     */
    int findSlot(VoxelWrapper w);

    /**
     * Adds a wrapper to the first empty slot.
     *
     * @return How much couldn't be put into the bag.
     */
    long add(final VoxelWrapper w, final long amount);

    /**
     * Returns whether this storage stores a given voxel type.
     */
    boolean has(VoxelWrapper b);

    /**
     * Gets the current amount of the voxel type.
     */
    long get(final VoxelWrapper w);

    /**
     * Return the amount of bits of a given voxel type that can be stored.
     */
    long queryRoom(final VoxelWrapper w);

    /**
     * Get the slot a given voxel wrapper is located in. If it has no slot this method
     * will return -1.
     */
    int getSlot(final VoxelWrapper w);

    /**
     * Sets a given slot to a given amount.
     */
    void setSlot(final int index, final VoxelWrapper w, final long amount);

    /**
     * Removes the contents of the given slot.
     */
    void clearSlot(final int index);

    /**
     * Get the voxel wrapper in a given slot.
     */
    VoxelWrapper getSlotContent(final int index);

    /**
     * Gets the voxel typ this storage is limited to.
     */
    VoxelType getType();

    /**
     * Set the voxel type this storage is limited to.
     */
    void setType(VoxelType voxelType);

    /**
     * Returns the amount of slots that have been occupied by a bit type.
     */
    int getOccupiedSlotCount();

    /**
     * Validates the
     */
    void validate();

    /**
     * Loads this storage from NBT.
     */
    void loadFromNBT(INBT nbt);

    /**
     * Creates NBT from this BitStorage that is loadable by
     * {@link #loadFromNBT(INBT)}
     */
    INBT toNBT();
}
