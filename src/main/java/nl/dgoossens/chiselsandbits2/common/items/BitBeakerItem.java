package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.List;

public class BitBeakerItem extends StorageItem {
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "bit_beaker.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu);
    }

    //This method was largely copied from BucketItem.java.
    //We use onItemRightClick instead of onItemUse because onItemUse only triggers when clicking a block. (which excludes liquids)
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if(!playerIn.isCrouching()) return new ActionResult<>(ActionResultType.PASS, itemstack);

        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.SOURCE_ONLY);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return new ActionResult<>(ActionResultType.PASS, itemstack);
        } else if (raytraceresult.getType() != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(ActionResultType.PASS, itemstack);
        } else {
            BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) raytraceresult;
            BlockPos blockpos = blockraytraceresult.getPos();
            if (worldIn.isBlockModifiable(playerIn, blockpos) && playerIn.canPlayerEdit(blockpos, blockraytraceresult.getFace(), itemstack)) {
                BlockState blockstate1 = worldIn.getBlockState(blockpos);
                if (blockstate1.getBlock() instanceof IBucketPickupHandler) {
                    Fluid fluid = ((IBucketPickupHandler)blockstate1.getBlock()).pickupFluid(worldIn, blockpos, blockstate1);
                    if (fluid != Fluids.EMPTY && fluid.isSource(fluid.getDefaultState())) {
                        playerIn.addStat(Stats.ITEM_USED.get(this));

                        SoundEvent soundevent = Fluids.EMPTY.getAttributes().getEmptySound();
                        if(soundevent == null) soundevent = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL;
                        playerIn.playSound(soundevent, 1.0F, 1.0F);
                        itemstack.getCapability(StorageCapabilityProvider.STORAGE).ifPresent(b -> {
                            try {
                                VoxelWrapper<Fluid> wrapper = VoxelWrapper.forFluid(fluid);
                                b.add(wrapper, (int) Math.pow(VoxelBlob.DIMENSION, 3));
                                //Set mode causes a capability update here.
                                ItemPropertyUtil.setSelectedVoxelWrapper(playerIn, itemstack, wrapper, true);
                            } catch(Exception x) {
                                x.printStackTrace();
                            }
                        });
                        return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
                    }
                }

                return new ActionResult<>(ActionResultType.FAIL, itemstack);
            } else {
                return new ActionResult<>(ActionResultType.FAIL, itemstack);
            }
        }
    }

    @Override
    public VoxelType getVoxelType() {
        return VoxelType.FLUIDSTATE;
    }
}
