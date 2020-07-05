package nl.dgoossens.chiselsandbits2.api.item.attributes;

import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.common.util.VoxelNBTConverter;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;

/**
 * A general interface for an item storing a voxel blob.
 */
public interface VoxelStorer {
    /**
     * Get the voxel blob stored by this voxel storer.
     */
    public default VoxelBlob getVoxelBlob(ItemStack item) {
        return VoxelNBTConverter.readFromNBT(item.getOrCreateChildTag(VoxelNBTConverter.NBT_BLOCKENTITYTAG));
    }

    /**
     * Set the voxel blob of this voxel storer.
     */
    public default void setVoxelBlob(ItemStack item, VoxelBlob vb) {
        item.setTagInfo(VoxelNBTConverter.NBT_BLOCKENTITYTAG, VoxelNBTConverter.writeToNBT(vb));

    }
}
