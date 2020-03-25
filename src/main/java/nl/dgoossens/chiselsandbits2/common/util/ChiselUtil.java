package nl.dgoossens.chiselsandbits2.common.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;

import javax.annotation.Nonnull;

/**
 * A util that handles everything related to the chiseleling process with some extra
 * general utility methods.
 */
public class ChiselUtil {
    public static boolean ACTIVELY_TRACING = false;
    public static final String NBT_BLOCKENTITYTAG = "BlockEntityTag";
    private static long memory = -1; //The amount of memory allocated to Minecraft.

    /**
     * Returns true if less than 1256 MB in memory is allocated
     * to Minecraft.
     */
    public static boolean isLowMemoryMode() {
        if (memory == -1)
            memory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // mb
        return memory < 1256;
    }

    /**
     * Plays the sound to be played when a chiseled block is modified.
     */
    public static void playModificationSound(final World world, final BlockPos pos, final boolean placement) {
        BlockState state = getChiseledTileMainState(world, pos);
        playModificationSound(world, pos, placement, state);
    }

    /**
     * Internal utility method for calculating what the primary blockstate of a block
     * is.
     */
    public static BlockState getChiseledTileMainState(final World world, final BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if(world.getTileEntity(pos) instanceof ChiseledBlockTileEntity) {
            int a = ((ChiseledBlockTileEntity) world.getTileEntity(pos)).getPrimaryBlock();
            if(a != VoxelBlob.AIR_BIT) state = BitUtil.getBlockState(a);
        }
        return state;
    }

    /**
     * Plays the sound to be played when a chiseled block is modified with a set state.
     */
    public static void playModificationSound(final World world, final BlockPos pos, final boolean placement, final BlockState state) {
        SoundType st = state.getSoundType();
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, placement ? st.getPlaceSound() : st.getBreakSound(), SoundCategory.BLOCKS, st.getVolume(), st.getPitch() * 0.9F);
    }

    /**
     * Returns whether or not this block at this position can be chiseled, uses an destroy block
     * event.
     */
    public static boolean canChiselPosition(final BlockPos pos, final PlayerEntity player, final BlockState state, final Direction face) {
        if (!(player.getHeldItemMainhand().getItem() instanceof IBitModifyItem))
            return false; //A valid item needs to be in the main hand!

        if (!player.getEntityWorld().getWorldBorder().contains(pos)) return false;
        if (!player.getEntityWorld().isBlockModifiable(player, pos)) return false;
        if (!player.canPlayerEdit(pos, face, player.getHeldItemMainhand())) return false;
        return ForgeEventFactory.onEntityDestroyBlock(player, pos, state);
    }

    /**
     * Raytraces from the player's eyes to a block, uses COLLIDER mode.
     */
    public static RayTraceResult rayTrace(final Entity entity) {
        Vec3d vec3d = entity.getEyePosition(1.0f);
        Vec3d vec3d1 = entity.getLook(1.0f);
        double d = Minecraft.getInstance().playerController.getBlockReachDistance();
        Vec3d vec3d2 = vec3d.add(vec3d1.x * d, vec3d1.y * d, vec3d1.z * d);
        ACTIVELY_TRACING = true;
        RayTraceResult r = entity.world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        ACTIVELY_TRACING = false;
        return r;
    }

    /**
     * Returns whether a target block is air or can be replaced.
     */
    public static boolean isBlockReplaceable(final World world, final BlockPos pos, final PlayerEntity player, final Direction face, final boolean destroy) {
        boolean isValid = world.isAirBlock(pos);

        //We see it as air if we can replace it.
        if (!isValid && world.getBlockState(pos).isReplaceable(new BlockItemUseContext(new ItemUseContext(player, Hand.MAIN_HAND, new BlockRayTraceResult(new Vec3d((double) pos.getX() + 0.5D + (double) face.getXOffset() * 0.5D, (double) pos.getY() + 0.5D + (double) face.getYOffset() * 0.5D, (double) pos.getZ() + 0.5D + (double) face.getZOffset() * 0.5D), face, pos, false))))) {
            if (destroy) world.destroyBlock(pos, true);
            isValid = true;
        }

        return isValid;
    }

    /**
     * Replaces a block at a position with a new chiseled block tile entity.
     */
    public static void replaceWithChiseled(final @Nonnull World world, final @Nonnull BlockPos pos, final @Nonnull PlayerEntity player, final BlockState originalState, final Direction face) {
        Block target = originalState.getBlock();
        BlockState placementState = ChiselsAndBits2.getInstance().getAPI().getRestrictions().getPlacementState(originalState);
        if(target.equals(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get()) || placementState == null) return;

        IFluidState fluid = world.getFluidState(pos);
        boolean isAir = isBlockReplaceable(world, pos, player, face, true);

        if (ChiselsAndBits2.getInstance().getAPI().getRestrictions().canChiselBlock(originalState) || isAir) {
            int blockId = BitUtil.getBlockId(placementState);
            world.setBlockState(pos, ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get().getDefaultState(), 3);
            final ChiseledBlockTileEntity te = (ChiseledBlockTileEntity) world.getTileEntity(pos);
            if (te != null) {
                if (!isAir) te.fillWith(blockId);
                else {
                    te.fillWith(VoxelBlob.AIR_BIT);
                    //If there was a fluid previously make this a fluid block instead of an air block.
                    if (fluid.isEmpty()) te.fillWith(VoxelBlob.AIR_BIT);
                    else te.fillWith(BitUtil.getFluidId(fluid));
                }
            }
        }
    }

    /**
     * Tests if a block can be replaced with a chiseled block. If this method succeeds then {@link #replaceWithChiseled(World, BlockPos, PlayerEntity, BlockState, Direction)} will be able to
     * create a tile entity, otherwise it won't be able to.
     */
    public static boolean isBlockChiselable(final @Nonnull World world, final @Nonnull BlockPos pos, final @Nonnull PlayerEntity player, final BlockState originalState, final Direction face) {
        Block target = originalState.getBlock();
        BlockState placementState = ChiselsAndBits2.getInstance().getAPI().getRestrictions().getPlacementState(originalState);
        if(target.equals(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get()) || placementState == null)
            return true;

        return ChiselsAndBits2.getInstance().getAPI().getRestrictions().canChiselBlock(originalState) || isBlockReplaceable(world, pos, player, face, false);
    }
}
