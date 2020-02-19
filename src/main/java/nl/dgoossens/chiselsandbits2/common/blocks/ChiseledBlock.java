package nl.dgoossens.chiselsandbits2.common.blocks;

import net.minecraft.block.*;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.items.ChiseledBlockItem;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class ChiseledBlock extends Block {
    public ChiseledBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ChiseledBlockTileEntity();
    }

    @Override //Our rendering shape is identical to the collision shape.
    public VoxelShape getRenderShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return getCollisionShape(state, worldIn, pos, ISelectionContext.dummy());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof ChiseledBlockTileEntity)) return VoxelShapes.empty();
        else return ((ChiseledBlockTileEntity) te).getCollisionShape();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof ChiseledBlockTileEntity)) return VoxelShapes.empty();
        else return ((ChiseledBlockTileEntity) te).getCachedShape();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED; //Set it to TESR only mode so there's no normal model.
    }

    //Redirect getSoundType to the primary block.
    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return getPrimaryState(world, pos).getSoundType(world, pos, entity);
    }

    //Redirect getSlipperiness to the primary block.
    @Override
    public float getSlipperiness(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return getPrimaryState(world, pos).getSlipperiness(world, pos, entity);
    }

    /**
     * Get the blockstate of the block that this chiseled block
     * is mainly made of.
     */
    public BlockState getPrimaryState(IBlockReader world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return null;
        return BitUtil.getBlockState(((ChiseledBlockTileEntity) te).getPrimaryBlock());
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        //Considering updating post placement doesn't occur that often. (not like every tick)
        //So we just always rebuild the chunk if we get this. We rebuild every time we chisel something so this isn't that performance heavy.
        TileEntity te = worldIn.getTileEntity(currentPos);
        if (te instanceof ChiseledBlockTileEntity)
            ((ChiseledBlockTileEntity) te).getRenderTracker().invalidate();
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    /**
     * Called before the Block is set to air in the world. Called regardless of if the player's tool can actually collect
     * this block.
     */
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if(player.isCreative()) {
            super.onBlockHarvested(worldIn, pos, state, player);
            return;
        }
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof ChiseledBlockTileEntity) {
            ChiseledBlockTileEntity cte = (ChiseledBlockTileEntity) tileentity;
            ItemEntity itementity = new ItemEntity(worldIn, pos.getX(), pos.getY(), pos.getZ(), cte.getItemStack());
            itementity.setDefaultPickupDelay();
            worldIn.addEntity(itementity);
        }

        super.onBlockHarvested(worldIn, pos, state, player);
    }

    /**
     * Handle block dropping properly from alternative sources, e.g. explosions.
     */
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        TileEntity tileentity = builder.get(LootParameters.BLOCK_ENTITY);
        if (tileentity instanceof ChiseledBlockTileEntity)
            builder = builder.withDynamicDrop(new ResourceLocation(ChiselsAndBits2.MOD_ID, "chiseled_block_drop"), (a, b) -> {
                ((ChiseledBlockTileEntity) tileentity).getItemStack();
            });
        return super.getDrops(state, builder);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        TileEntity te = world.getTileEntity(pos);
        if(te instanceof ChiseledBlockTileEntity)
            return ((ChiseledBlockTileEntity) te).getItemStack();
        return ItemStack.EMPTY;
    }

    //--- PARTICLE EFFECTS ---
    @Override
    public boolean addLandingEffects(BlockState state1, ServerWorld worldserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        try {
            //Add landing effects of the bit type right below the entity.
            BitLocation location = new BitLocation(entity);
            VoxelBlob tar = ((ChiseledBlockTileEntity) worldserver.getTileEntity(location.getBlockPos())).getVoxelReference().getVoxelBlob();
            int i = tar.getSafe(location.bitX, location.bitY, location.bitZ);
            if(i == VoxelBlob.AIR_BIT)
                return true; //No particles if you land on air. (on edges of blocks)

            switch(VoxelType.getType(i)) {
                case BLOCKSTATE:
                    worldserver.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, BitUtil.getBlockState(i)), entity.getPosX(), entity.getPosY(), entity.getPosZ(), numberOfParticles, 0.0, 0.0, 0.0, 1);
                    return true;
                case FLUIDSTATE:
                    worldserver.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, BitUtil.getFluidState(i).getBlockState()), entity.getPosX(), entity.getPosY(), entity.getPosZ(), numberOfParticles, 0.0, 0.0, 0.0, 1);
                    return true;
                case COLOURED:
                    Color c = BitUtil.getColourState(i);
                    worldserver.spawnParticle(new RedstoneParticleData(((float)c.getRed())/255.0f, ((float)c.getGreen())/255.0f, ((float)c.getBlue())/255.0f, ((float)c.getAlpha())/255.0f), entity.getPosX(), entity.getPosY(), entity.getPosZ(), numberOfParticles, 0.0, 0.0, 0.0, 1);
                    return true;
            }
        } catch(Exception x) {
            return true; //Exception = no particles
        }
        return false;
    }

    @Override
    public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
        try {
            VoxelBlob tar = ((ChiseledBlockTileEntity) world.getTileEntity(pos)).getVoxelReference().getVoxelBlob();
            int i = tar.getMostCommonStateId();
            if(i == VoxelBlob.AIR_BIT)
                return true; //No most common state (which shouldn't happen really), no particles.

            switch(VoxelType.getType(i)) {
                case BLOCKSTATE:
                    addBlockDestroyEffects(manager, world, pos, BitUtil.getBlockState(i));
                    return true;
                case FLUIDSTATE:
                    addBlockDestroyEffects(manager, world, pos, BitUtil.getBlockState(i));
                    return true;
                case COLOURED:
                    Color c = BitUtil.getColourState(i);
                    addBlockDestroyEffects(manager, world, pos, Blocks.WHITE_CONCRETE.getDefaultState(), ((float)c.getRed())/255.0f, ((float)c.getGreen())/255.0f, ((float)c.getBlue())/255.0f, ((float)c.getAlpha())/255.0f);
                    return true;
            }
        } catch(Exception x) {
            return true; //Exception = no particles
        }
        return false;
    }

    //Add the block destroy effects exactly as the ParticleManager would.
    private void addBlockDestroyEffects(ParticleManager manager, World world, BlockPos pos, BlockState state) {
        if (!state.isAir(world, pos) && !state.addDestroyEffects(world, pos, manager))
            addBlockDestroyEffects(world, pos, state, (p) -> manager.addEffect(p));
    }

    //Add the block destroy effects exactly as the ParticleManager would, but with a custom colour.
    private void addBlockDestroyEffects(ParticleManager manager, World world, BlockPos pos, BlockState state, float red, float green, float blue, float alpha) {
        if (!state.isAir(world, pos) && !state.addDestroyEffects(world, pos, manager))
            addBlockDestroyEffects(world, pos, state, (p) -> {
                p.setColor(red, green, blue);
                manager.addEffect(p);
            });
    }

    private void addBlockDestroyEffects(World world, BlockPos pos, BlockState state, Consumer<Particle> sender) {
        //We take the shape from the chiseled block so the particles render in the correct locations.
        VoxelShape voxelshape = world.getBlockState(pos).getShape(world, pos);
        double d0 = 0.25D;
        voxelshape.forEachBox((p_199284_3_, p_199284_5_, p_199284_7_, p_199284_9_, p_199284_11_, p_199284_13_) -> {
            double d1 = Math.min(1.0D, p_199284_9_ - p_199284_3_);
            double d2 = Math.min(1.0D, p_199284_11_ - p_199284_5_);
            double d3 = Math.min(1.0D, p_199284_13_ - p_199284_7_);
            int i = Math.max(2, MathHelper.ceil(d1 / 0.25D));
            int j = Math.max(2, MathHelper.ceil(d2 / 0.25D));
            int k = Math.max(2, MathHelper.ceil(d3 / 0.25D));

            for(int l = 0; l < i; ++l) {
                for(int i1 = 0; i1 < j; ++i1) {
                    for(int j1 = 0; j1 < k; ++j1) {
                        double d4 = ((double)l + 0.5D) / (double)i;
                        double d5 = ((double)i1 + 0.5D) / (double)j;
                        double d6 = ((double)j1 + 0.5D) / (double)k;
                        double d7 = d4 * d1 + p_199284_3_;
                        double d8 = d5 * d2 + p_199284_5_;
                        double d9 = d6 * d3 + p_199284_7_;
                        Particle particle = (new DiggingParticle(world, (double)pos.getX() + d7, (double)pos.getY() + d8, (double)pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, state)).setBlockPos(pos);
                        sender.accept(particle);
                    }
                }
            }
        });
    }

    @Override
    public boolean addHitEffects(BlockState state, World world, RayTraceResult target, ParticleManager manager) {
        try {
            BitLocation location = new BitLocation((BlockRayTraceResult) target, true, BitOperation.REMOVE);
            VoxelBlob tar = ((ChiseledBlockTileEntity) world.getTileEntity(location.getBlockPos())).getVoxelReference().getVoxelBlob();
            int i = tar.getSafe(location.bitX, location.bitY, location.bitZ);

            //If you're not hitting a bit we take the most common type.
            if(i == VoxelBlob.AIR_BIT)
                i = tar.getMostCommonStateId();
            if(i == VoxelBlob.AIR_BIT)
                return true;

            switch(VoxelType.getType(i)) {
                case BLOCKSTATE:
                    addBlockHitEffects(manager, world, ((BlockRayTraceResult) target).getPos(), ((BlockRayTraceResult) target).getFace(), BitUtil.getBlockState(i));
                    return true;
                case FLUIDSTATE:
                    addBlockHitEffects(manager, world, ((BlockRayTraceResult) target).getPos(), ((BlockRayTraceResult) target).getFace(), BitUtil.getFluidState(i).getBlockState());
                    return true;
                case COLOURED:
                    Color c = BitUtil.getColourState(i);
                    addBlockHitEffects(manager, world, ((BlockRayTraceResult) target).getPos(), ((BlockRayTraceResult) target).getFace(), Blocks.WHITE_CONCRETE.getDefaultState(), ((float)c.getRed())/255.0f, ((float)c.getGreen())/255.0f, ((float)c.getBlue())/255.0f, ((float)c.getAlpha())/255.0f);
                    return true;
            }
        } catch(Exception x) {
            return true; //Exception = no particles
        }
        return false;
    }

    //Add the block hit effects exactly as the ParticleManager would, but with a custom blockstate type and no colour.
    private void addBlockHitEffects(ParticleManager manager, World world, BlockPos pos, Direction side, BlockState blockstate) {
        if (blockstate.getRenderType() != BlockRenderType.INVISIBLE)
            manager.addEffect(buildHitParticle(world, pos, side, blockstate));
    }

    //Add the block hit effects exactly as the ParticleManager would, but with a custom blockstate type.
    private void addBlockHitEffects(ParticleManager manager, World world, BlockPos pos, Direction side, BlockState blockstate, float red, float green, float blue, float alpha) {
        if (blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
            Particle particle = buildHitParticle(world, pos, side, blockstate);
            particle.setColor(red, green, blue);
            manager.addEffect(particle);
        }
    }

    //Builds the hit particle
    private Particle buildHitParticle(World world, BlockPos pos, Direction side, BlockState blockstate) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        float f = 0.1F;
        //We take the bounding box from the chiseled block so the particles render in the correct locations.
        AxisAlignedBB axisalignedbb = world.getBlockState(pos).getShape(world, pos).getBoundingBox();
        Random rand = new Random();
        double d0 = (double) i + rand.nextDouble() * (axisalignedbb.maxX - axisalignedbb.minX - (double) 0.2F) + (double) 0.1F + axisalignedbb.minX;
        double d1 = (double) j + rand.nextDouble() * (axisalignedbb.maxY - axisalignedbb.minY - (double) 0.2F) + (double) 0.1F + axisalignedbb.minY;
        double d2 = (double) k + rand.nextDouble() * (axisalignedbb.maxZ - axisalignedbb.minZ - (double) 0.2F) + (double) 0.1F + axisalignedbb.minZ;
        if (side == Direction.DOWN)
            d1 = (double) j + axisalignedbb.minY - (double) 0.1F;
        if (side == Direction.UP)
            d1 = (double) j + axisalignedbb.maxY + (double) 0.1F;
        if (side == Direction.NORTH)
            d2 = (double) k + axisalignedbb.minZ - (double) 0.1F;
        if (side == Direction.SOUTH)
            d2 = (double) k + axisalignedbb.maxZ + (double) 0.1F;
        if (side == Direction.WEST)
            d0 = (double) i + axisalignedbb.minX - (double) 0.1F;
        if (side == Direction.EAST)
            d0 = (double) i + axisalignedbb.maxX + (double) 0.1F;

        return (new DiggingParticle(world, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate)).setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F);
    }

    @Override
    public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
        try {
            //Add running effects of the bit type right below the entity.
            Vec3d vec3d = entity.getMotion();
            BitLocation location = new BitLocation(entity);
            VoxelBlob tar = ((ChiseledBlockTileEntity) world.getTileEntity(location.getBlockPos())).getVoxelReference().getVoxelBlob();
            int i = tar.getSafe(location.bitX, location.bitY, location.bitZ);
            if(i == VoxelBlob.AIR_BIT)
                return true; //Running particles are not shown if you are not above a bit

            Random random = new Random();
            switch(VoxelType.getType(i)) {
                case BLOCKSTATE:
                    world.addParticle(new BlockParticleData(ParticleTypes.BLOCK, BitUtil.getBlockState(i)), entity.getPosX() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, entity.getPosY() + 0.1D, entity.getPosZ() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
                    return true;
                case FLUIDSTATE:
                    world.addParticle(new BlockParticleData(ParticleTypes.BLOCK, BitUtil.getFluidState(i).getBlockState()), entity.getPosX() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, entity.getPosY() + 0.1D, entity.getPosZ() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
                    return true;
                case COLOURED:
                    Color c = BitUtil.getColourState(i);
                    world.addParticle(new RedstoneParticleData(((float)c.getRed())/255.0f, ((float)c.getGreen())/255.0f, ((float)c.getBlue())/255.0f, ((float)c.getAlpha())/255.0f), entity.getPosX() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, entity.getPosY() + 0.1D, entity.getPosZ() + ((double)random.nextFloat() - 0.5D) * (double)entity.getSize(entity.getPose()).width, vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
                    return true;
            }
        } catch(Exception x) {
            return true; //No particles in event of an exception.
        }
        return false;
    }
}
