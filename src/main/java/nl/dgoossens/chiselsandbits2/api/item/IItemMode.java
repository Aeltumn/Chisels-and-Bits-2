package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.ChiselIterator;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;

import javax.annotation.Nullable;

/**
 * A generic item mode which can be
 */
public interface IItemMode {
    /**
     * Get the name of this item mode as it can be stored in NBT.
     */
    String getName();

    /**
     * Get the localized key from this Item Mode.
     */
    String getLocalizedName();

    /**
     * Return getName() but without the type in front.
     */
    String getTypelessName();

    /**
     * Get the type of this item mode.
     */
    IItemModeType getType();

    /**
     * Get the iterator that iterates over the bits in this selection.
     * Only applicable if the type is {@link nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType#CHISEL}!
     *
     * @param bitPosition The position of the bit being chiseled. The block being chiseled should be passed through the voxel source.
     * @param source Optionally the block actually being chiseled. Some modes won't need this but the ones that depend on the block do.
     */
    public default ChiselIterator getIterator(final BlockPos bitPosition, final Direction side, final BitOperation place, @Nullable final IVoxelSrc source) {
        return getIterator(bitPosition, side, place, source, null, null);
    }

    /**
     * Variant of the normal {@link #getIterator(BlockPos, Direction, BitOperation, IVoxelSrc)} where an entire
     * region can be passed to look at.
     * @param from Start of the iterator's area
     * @param to End of the iterator's area
     */
    public default ChiselIterator getIterator(final BlockPos bitPosition, final Direction side, final BitOperation place, @Nullable final IVoxelSrc source, @Nullable final BitLocation from, @Nullable final BitLocation to) {
        if(getType().equals(ItemModeType.CHISEL)) throw new UnsupportedOperationException("Mode "+toString()+" doesn't have an implementation for its iterator!");
        else return null; //Shouldn't ask a non-chisel mode for it's CHISEL-iterator.
    }

    /**
     * Get the resource location for the icon.
     */
    public default ResourceLocation getIconResourceLocation() {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/modes/" + getTypelessName().toLowerCase());
    }

    /**
     * How many pixels the png file for this icon is wide.
     */
    public default int getTextureWidth() {
        return 16;
    }

    /**
     * How many pixels the png file for this icon is high.
     */
    public default int getTextureHeight() {
        return 16;
    }
}
