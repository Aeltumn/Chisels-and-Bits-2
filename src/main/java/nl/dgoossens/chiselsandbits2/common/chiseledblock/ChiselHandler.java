package nl.dgoossens.chiselsandbits2.common.chiseledblock;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IRotatableItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IVoxelStorer;
import nl.dgoossens.chiselsandbits2.client.cull.DummyBlockReader;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel.ChiselIterator;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.ExtendedVoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;
import nl.dgoossens.chiselsandbits2.common.impl.item.*;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.VoxelRegionSrc;
import nl.dgoossens.chiselsandbits2.common.items.ChiselMimicItem;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.items.TypedItem;
import nl.dgoossens.chiselsandbits2.common.network.client.CPlaceBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CWrenchBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CRotateItemPacket;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
import nl.dgoossens.chiselsandbits2.common.network.client.CChiselBlockPacket;
import nl.dgoossens.chiselsandbits2.common.util.InventoryUtils;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.util.RotationUtil;

/**
 * Handles incoming packets that relate to interacting with voxelblobs.
 */
public class ChiselHandler {
    /**
     * Handles an incoming {@link CChiselBlockPacket} packet.
     */
    public static void handle(final CChiselBlockPacket pkt, final ServerPlayerEntity player) {
        final ItemStack stack = player.getHeldItemMainhand();
        if(!(stack.getItem() instanceof ChiselMimicItem))
            return;

        ChiselMimicItem tit = (ChiselMimicItem) stack.getItem();
        if(!tit.canPerformModification(IBitModifyItem.ModificationType.BUILD) && !tit.canPerformModification(IBitModifyItem.ModificationType.EXTRACT))
            return; //Make sure this item can do the operations

        final IItemMode mode = tit.getSelectedMode(stack);
        final World world = player.world;
        final InventoryUtils.CalculatedInventory inventory = InventoryUtils.buildInventory(player);

        //If world.getServer() is null we return to make the "if (world.getServer().isBlockProtected(world, pos, player))" never fail.
        if (world.getServer() == null)
            return;

        //Cancel chiseling early on if possible (but not if creative)
        if (inventory.getAvailableDurability() <= 0 && !player.isCreative()) {
            player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.need_chisel"), true);
            return;
        }

        //Default is -1 for remove operations, we only get a placed bit when not removing
        int placedBit = -1;
        if(!pkt.operation.equals(BitOperation.REMOVE)) {
            //If this is a locked morphing bit place that specific item
            if(stack.getItem() instanceof MorphingBitItem && ((MorphingBitItem) stack.getItem()).isLocked(stack)) placedBit = ((MorphingBitItem) stack.getItem()).getSelected(stack).getId();
            else placedBit = ItemPropertyUtil.getGlobalSelectedVoxelWrapper(player).getPlacementBitId(buildContext(player, pkt.to.blockPos, pkt.side)); //We'll use the block position of the target location

            //If we couldn't find a selected type, don't chisel.
            if (placedBit == VoxelBlob.AIR_BIT) {
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.no_selected_type"), true);
                return;
            }

            inventory.trackMaterialUsage(VoxelWrapper.forAbstract(placedBit));

            //Cancel chiseling early on if possible (but not if creative)
            //This shouldn't ever happen because you can't have a material selected that you don't have.
            //Well actually this can happen if you drop the bags and instantly click before the routine updating catches up to you. tl;dr this can only happen if the cached value isn't updated on time
            if (inventory.getAvailableMaterial() <= 0 && !player.isCreative()) {
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.need_bits"), true);
                return;
            }
        }

        final BlockPos from = pkt.from.blockPos;
        final BlockPos to = pkt.to.blockPos;

        final int maxX = Math.max(from.getX(), to.getX());
        final int maxY = Math.max(from.getY(), to.getY());
        final int maxZ = Math.max(from.getZ(), to.getZ());

        ChiselsAndBits2.getInstance().getUndoTracker().beginGroup(player);

        try {
            //Uses to be added to the statistic
            for (int xOff = Math.min(from.getX(), to.getX()); xOff <= maxX; ++xOff) {
                for (int yOff = Math.min(from.getY(), to.getY()); yOff <= maxY; ++yOff) {
                    for (int zOff = Math.min(from.getZ(), to.getZ()); zOff <= maxZ; ++zOff) {
                        final BlockPos pos = new BlockPos(xOff, yOff, zOff);
                        //If we can't chisel here, don't chisel.
                        //Check if a valid location is being chiseled
                        final BlockState state = player.world.getBlockState(pos);

                        //Check if this block is mutable
                        if (!state.isReplaceable(buildContext(player, pos, pkt.side)) && !ChiselsAndBits2.getInstance().getAPI().getRestrictions().canChiselBlock(state))
                            return;
                        if (!ChiselUtil.canChiselPosition(pos, player, state, pkt.side))
                            return;
                        if (world.getServer().isBlockProtected(world, pos, player))
                            continue;

                        //Replace the block with a chiseled block.
                        ChiselUtil.replaceWithChiseled(world, pos, player, world.getBlockState(pos), pkt.side);

                        final TileEntity te = world.getTileEntity(pos);
                        if (te instanceof ChiseledBlockTileEntity) {
                            final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;
                            final VoxelBlob vb = tec.getVoxelBlob();
                            final ChiselIterator i = ChiselUtil.getIterator(mode, pkt, new VoxelRegionSrc(player, world, pos, 1), pos, pkt.operation);

                            //Handle the operation
                            switch (pkt.operation) {
                                case SWAP:
                                case PLACE: {
                                    inventory.setEffectState(BitUtil.getBlockState(placedBit)); //TODO add support for fluids/coloured bits
                                    while (i.hasNext())
                                        if(inventory.placeBit(vb, i.x(), i.y(), i.z(), pkt.operation, placedBit) != 0) break;
                                }
                                break;
                                case REMOVE: {
                                    while (i.hasNext())
                                        if(inventory.removeBit(vb, i.x(), i.y(), i.z())) break;
                                    inventory.setEffectState(inventory.getMostCommonState());
                                }
                                break;
                            }

                            //Actually apply the operation.
                            tec.completeEditOperation(player, vb, true);

                            //Play effects if applicable
                            inventory.playEffects(world, pos);
                        }
                    }
                }
            }
        } finally {
            inventory.apply();
            //Increment item usage statistic
            player.getStats().increment(player, Stats.ITEM_USED.get(player.getHeldItemMainhand().getItem()), 1);
            ChiselsAndBits2.getInstance().getUndoTracker().endGroup(player);
        }
    }

    private static BlockItemUseContext buildContext(final ServerPlayerEntity player, final BlockPos pos, final Direction side) {
        return new BlockItemUseContext(new ItemUseContext(player, Hand.MAIN_HAND, new BlockRayTraceResult(new Vec3d((double) pos.getX() + 0.5D + (double) side.getXOffset() * 0.5D, (double) pos.getY() + 0.5D + (double) side.getYOffset() * 0.5D, (double) pos.getZ() + 0.5D + (double) side.getZOffset() * 0.5D), side, pos, false)));
    }

    /**
     * Handles an incoming {@link CRotateItemPacket} packet.
     */
    public static void handle(final CRotateItemPacket pkt, final ServerPlayerEntity player) {
        //Try to rotate both hands if possible
        ItemStack it = player.getHeldItemMainhand();
        if(it.getItem() instanceof IRotatableItem)
            ((IRotatableItem) it.getItem()).rotate(it, pkt.getAxis(), pkt.isClockwise());
        else {
            it = player.getHeldItemOffhand();
            if(it.getItem() instanceof IRotatableItem)
                ((IRotatableItem) it.getItem()).rotate(it, pkt.getAxis(), pkt.isClockwise());
        }
    }

    /**
     * Handles an incoming {@link CPlaceBlockPacket} packet.
     */
    public static void handle(final CPlaceBlockPacket pkt, final ServerPlayerEntity player) {
        final ItemStack stack = player.getHeldItemMainhand();
        if(!(stack.getItem() instanceof IVoxelStorer) || !(stack.getItem() instanceof IBitModifyItem))
            return;

        if (!((IBitModifyItem) stack.getItem()).canPerformModification(IBitModifyItem.ModificationType.PLACE))
            return; //Make sure this item can do the operations

        final IVoxelStorer it = (IVoxelStorer) stack.getItem();
        final World world = player.world;
        final PlayerItemMode mode = player.getCapability(PlayerItemModeCapabilityProvider.PIMM).map(PlayerItemModeManager::getChiseledBlockMode).orElse((PlayerItemMode) ItemModeType.CHISELED_BLOCK.getDefault());
        final Direction face = pkt.side;
        final NBTBlobConverter nbt = new NBTBlobConverter();
        nbt.readChiselData(stack.getChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());

        //Check if we can place it here
        BlockPos actualPos = pkt.pos; //Placement block for non-offgird placement
        boolean canPlace = true;
        if (player.isCrouching() && !ClientItemPropertyUtil.getChiseledBlockMode().equals(PlayerItemMode.CHISELED_BLOCK_GRID)) {
            if (!BlockPlacementLogic.isPlaceableOffgrid(player, player.world, face, pkt.location, player.getHeldItemMainhand()))
                canPlace = false;
        } else {
            if((!ChiselUtil.isBlockReplaceable(player.world, actualPos, player, face, false) && ClientItemPropertyUtil.getChiseledBlockMode() == PlayerItemMode.CHISELED_BLOCK_GRID) || (!(player.world.getTileEntity(actualPos) instanceof ChiseledBlockTileEntity) && !BlockPlacementLogic.isNormallyPlaceable(player, player.world, actualPos, face, nbt, mode)))
                actualPos = actualPos.offset(face);

            if(!BlockPlacementLogic.isNormallyPlaceable(player, player.world, actualPos, face, nbt, mode))
                canPlace = false;
        }
        if(!canPlace) {
            player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.not_placeable"), true);
            return;
        }

        if(player.isCrouching()) {
            //Offgrid mode, place in all blockpositions concerned
            final ExtendedVoxelBlob evb = new ExtendedVoxelBlob(3, 3, 3, -1, -1, -1);
            final VoxelBlob placedBlob = it.getVoxelBlob(stack);
            evb.insertBlob(0, 0, 0, placedBlob);
            final IntegerBox bounds = placedBlob.getBounds();
            final BlockPos partialOffset = BlockPlacementLogic.getPartialOffset(pkt.side, new BlockPos(pkt.location.bitX, pkt.location.bitY, pkt.location.bitZ), bounds);
            evb.shift(partialOffset.getX(), partialOffset.getY(), partialOffset.getZ());

            for(BlockPos pos : evb.listBlocks()) {
                final VoxelBlob slice = evb.getSubVoxel(pos.getX(), pos.getY(), pos.getZ());
                pos = pos.add(pkt.location.blockPos);
                //If we can't chisel here, don't chisel.
                if (world.getServer().isBlockProtected(world, pos, player))
                    continue;

                //Replace the block with a chiseled block.
                ChiselUtil.replaceWithChiseled(world, pos, player, world.getBlockState(pos), pkt.side);

                final TileEntity te = world.getTileEntity(pos);
                if (te instanceof ChiseledBlockTileEntity) {
                    final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;

                    switch(mode) {
                        case CHISELED_BLOCK_FIT:
                        case CHISELED_BLOCK_MERGE:
                        case CHISELED_BLOCK_OVERLAP:
                            final VoxelBlob vb = tec.getVoxelBlob();
                            if(mode.equals(PlayerItemMode.CHISELED_BLOCK_OVERLAP))
                                vb.overlap(slice);
                            else
                                vb.merge(slice);

                            tec.completeEditOperation(player, vb, false);
                            break;
                    }
                }
            }

            if(!player.isCreative())
                stack.setCount(stack.getCount() - 1);
            ChiselUtil.playModificationSound(world, pkt.location.blockPos, true); //Placement can play sound normally as block should be set already.
        } else {
            //Normal mode, place in this block position
            ChiselUtil.replaceWithChiseled(player.world, actualPos, player, world.getBlockState(actualPos), pkt.side);

            final TileEntity te = world.getTileEntity(actualPos);
            if (te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;
                switch(mode) {
                    case CHISELED_BLOCK_GRID:
                        tec.setBlob(it.getVoxelBlob(stack));
                        break;
                    case CHISELED_BLOCK_FIT:
                    case CHISELED_BLOCK_MERGE:
                    case CHISELED_BLOCK_OVERLAP:
                        final VoxelBlob vb = tec.getVoxelBlob();
                        if(mode.equals(PlayerItemMode.CHISELED_BLOCK_OVERLAP))
                            vb.overlap(it.getVoxelBlob(stack));
                        else
                            vb.merge(it.getVoxelBlob(stack));
                        tec.completeEditOperation(player, vb, false);
                        break;
                }
                if(!player.isCreative())
                    stack.setCount(stack.getCount() - 1);
                ChiselUtil.playModificationSound(world, actualPos, true); //Placement can play sound normally as block should be set already.
            }
        }
    }

    /**
     * Handles an incoming {@link CWrenchBlockPacket} packet.
     */
    public static void handle(final CWrenchBlockPacket pkt, final ServerPlayerEntity player) {
        final ItemStack stack = player.getHeldItemMainhand();
        if(!(stack.getItem() instanceof TypedItem) && !(stack.getItem() instanceof IBitModifyItem))
            return;

        final TypedItem tit = (TypedItem) stack.getItem();
        if(!((IBitModifyItem) tit).canPerformModification(IBitModifyItem.ModificationType.ROTATE) && !((IBitModifyItem) tit).canPerformModification(IBitModifyItem.ModificationType.MIRROR))
            return; //Make sure this item can do the operations

        final World world = player.world;
        final BlockPos pos = pkt.pos;
        final BlockState state = world.getBlockState(pos);
        final Direction face = pkt.side;
        final IItemMode mode = tit.getSelectedMode(stack);

        if (!ChiselUtil.canChiselPosition(pos, player, state, face)) return;

        if(!(mode instanceof ItemMode))
            return; //We don't accept other modes for rotation.

        if (mode.equals(ItemMode.WRENCH_MIRROR)) {
            if (!RotationUtil.hasMirrorableState(state)) {
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.not_mirrorable"), true);
                return;
            }
        } else if(mode.equals(ItemMode.WRENCH_ROTATE) || mode.equals(ItemMode.WRENCH_ROTATECCW)) {
            if (!RotationUtil.hasRotatableState(state)) {
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.not_rotatable"), true);
                return;
            }
        }

        //Custom chiseled block
        if(state.getBlock() instanceof ChiseledBlock) {
            final TileEntity te = world.getTileEntity(pos);
            if(te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity cte = (ChiseledBlockTileEntity) te;
                VoxelBlob voxel = cte.getVoxelBlob();
                if(mode.equals(ItemMode.WRENCH_MIRROR)) voxel = voxel.mirror(pkt.side.getAxis());
                else if(mode.equals(ItemMode.WRENCH_ROTATE)) voxel = voxel.spin(pkt.side.getAxis());
                else if(mode.equals(ItemMode.WRENCH_ROTATECCW)) voxel = voxel.spinCCW(pkt.side.getAxis());
                cte.setBlob(voxel);
            }
        } else {
            //Other rotatable blocks
            DummyBlockReader dummyWorld = new DummyBlockReader() {
                @Override
                public BlockState getBlockState(BlockPos pos) {
                    if (pos.equals(BlockPos.ZERO)) return state;
                    return super.getBlockState(pos);
                }
            };
            if (state.getBlockHardness(dummyWorld, BlockPos.ZERO) < 0) return; //Can't move unbreakable blocks. (they have -1 hardness)

            if(mode.equals(ItemMode.WRENCH_MIRROR)) world.setBlockState(pos, state.mirror(RotationUtil.getMirror(pkt.side.getAxis())));
            else world.setBlockState(pos, state.rotate(world, pos, mode.equals(ItemMode.WRENCH_ROTATECCW) ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90));
        }

        //Reduce wrench durability
        player.getHeldItemMainhand().damageItem(1, player, (p) -> p.sendBreakAnimation(Hand.MAIN_HAND));
        player.getStats().increment(player, Stats.ITEM_USED.get(ChiselsAndBits2.getInstance().getRegister().WRENCH.get()), 1);
        ChiselUtil.playModificationSound(world, pos, true); //Wrench can play sound of the block as it has been set for this world.
    }
}
