package nl.dgoossens.chiselsandbits2.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.api.bit.RestrictionAPI;
import nl.dgoossens.chiselsandbits2.api.block.BitAccess;

import java.util.Collection;
import java.util.Optional;

/**
 * The Chisels & Bits API is a way for any mod (including C&B2 itself)
 * to interact with C&B tiles and modify them.
 * Much of C&Bs functionality can also be found in the various utility classes
 * in the common.utils package.
 * <p>
 * If you'd like more of C&B to be accessible through the API, like how the API allows adding custom item modes, you can
 * make an issue requesting it. No part of C&B should be hidden from other mods wanting to use it and most enums should
 * be extendable. However, something like VoxelType can't be extended and no PR trying to do so will be merged.
 */
public interface ChiselsAndBitsAPI {
    /**
     * Get an entry point to a given block position in a given world.
     * The BitAccess allows access and easy manipulation of the voxel data of a given
     * chiseled block.
     * The player is required to see if this block is a replaceable block (like air, water or grass) in which
     * case you can still get a bit access despite there being no chiseled block in the location yet.
     */
    Optional<BitAccess> getBitAccess(final PlayerEntity player, final World world, final BlockPos pos);

    /**
     * Get the API that manages blocked or restricted blocks for rotating or using in chiseled blocks. Here blocks can be
     * specially marked as chiselable regardless of the block shape or properties.
     */
    RestrictionAPI getRestrictions();

    /**
     * Ignore a blockstate property from being seen as a unique chiselable type, it will be seen as
     * its default property. This should be used for questionable block properties, some examples:
     * - Leaf decay distance
     * <p>
     * A good test to use to figure out if a blockstate should be ignored is as follows:
     * If you had a block filled completely with logs on different axis, you wouldn't want the block to turn into a full
     * log block with a single axis. So axis should be a property of a chiseled bit.
     * <p>
     * If you had a block filled with leaves with varying distance values, you would want the block to turn into a full
     * block regardless of the distance values of the parts of the block. So DISTANCE_1_7 should be ignored.
     * <p>
     * Furthermore, different blockstates values also cause the face to be rendered, so only blockstates that can alter
     * the rotation or rendering of the block should go un-ignored ideally.
     */
    void addIgnoredBlockState(final IProperty<?> property);

    /**
     * Get the list of all ignored block states.
     */
    Collection<IProperty<?>> getIgnoredBlockStates();
}
