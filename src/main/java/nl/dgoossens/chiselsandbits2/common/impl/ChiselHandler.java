package nl.dgoossens.chiselsandbits2.common.impl;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.item.ItemMode;
import nl.dgoossens.chiselsandbits2.api.item.StandardTypedItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.BitModifyItem;
import nl.dgoossens.chiselsandbits2.api.bit.BitOperation;
import nl.dgoossens.chiselsandbits2.api.item.attributes.RotatableItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.VoxelStorer;
import nl.dgoossens.chiselsandbits2.api.voxel.ExtendedAxisAlignedBB;
import nl.dgoossens.chiselsandbits2.client.cull.DummyBlockReader;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.util.VoxelNBTConverter;
import nl.dgoossens.chiselsandbits2.api.iterator.ChiselIterator;
import nl.dgoossens.chiselsandbits2.api.voxel.ExtendedVoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.util.BlockPlacementLogic;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeTypes;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModes;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemModeCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemModeManager;
import nl.dgoossens.chiselsandbits2.common.items.SculptItem;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.network.client.CChiselBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CPlaceBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CRotateItemPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CWrenchBlockPacket;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
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
        if (!(stack.getItem() instanceof SculptItem))
            return;

        SculptItem tit = (SculptItem) stack.getItem();
        if (!tit.canPerformModification(BitModifyItem.ModificationType.BUILD) && !tit.canPerformModification(BitModifyItem.ModificationType.EXTRACT))
            return; //Make sure this item can do the operations

        final ItemMode mode = tit.getSelectedMode(stack);
        final World world = player.world;
        final InventoryUtils.CalculatedInventory inventory = InventoryUtils.buildInventory(player);

        //If world.getServer() is null we return to make the "if (world.getServer().isBlockProtected(world, pos, player))" never fail.
        if (world.getServer() == null)
            return;

        //Cancel chiseling early on if possible (but not if creative)
        if (inventory.getAvailableDurability() <= 0 && !player.isCreative()) {
            player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.need_chisel"), true);
            return;
        }

        //Default is -1 for remove operations, we only get a placed bit when not removing
        int placedBit = -1;
        if (!pkt.operation.equals(BitOperation.REMOVE)) {
            //If this is a locked morphing bit place that specific item
            if (stack.getItem() instanceof MorphingBitItem && ((MorphingBitItem) stack.getItem()).isLocked(stack)) placedBit = ((MorphingBitItem) stack.getItem()).getSelected(stack).getId();
            else placedBit = ItemPropertyUtil.getGlobalSelectedVoxelWrapper(player).getPlacementBitId(buildContext(player, pkt.to.blockPos, pkt.side)); //We'll use the block position of the target location

            //If we couldn't find a selected type, don't chisel.
            if (placedBit == VoxelBlob.AIR_BIT) {
                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.no_selected_type"), true);
                return;
            }

            inventory.trackMaterialUsage(VoxelWrapper.forAbstract(placedBit));

            //Cancel chiseling early on if possible (but not if creative)
            //This shouldn't ever happen because you can't have a material selected that you don't have.
            //Well actually this can happen if you drop the bags and instantly click before the routine updating catches up to you. tl;dr this can only happen if the cached value isn't updated on time
            if (inventory.getAvailableMaterial() <= 0 && !player.isCreative()) {
                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.need_bits"), true);
                return;
            }
        }

        //If this is drawn region we check if the selected area isn't too big
        if (mode.equals(ItemModes.CHISEL_DRAWN_REGION)) {
            ExtendedAxisAlignedBB bb = ChiselUtil.getBoundingBox(pkt.from, pkt.to, mode);
            if (bb.isLargerThan(ChiselsAndBits2.getInstance().getConfig().maxDrawnRegionSize.get())) {
                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.drawn_region_too_big"), true);
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
                            //Drawn region wants the actual block position whilst all other iterators want the bit position.
                            final ChiselIterator i = mode.getIterator(mode.equals(ItemModes.CHISEL_DRAWN_REGION) ? pos : pkt.from.getBitPos(),
                                    pkt.side, pkt.operation, (ChiseledBlockTileEntity) te, pkt.from, pkt.to);

                            //Handle the operation
                            switch (pkt.operation) {
                                case SWAP:
                                case PLACE: {
                                    switch(VoxelType.getType(placedBit)) {
                                        case BLOCKSTATE:
                                            inventory.setEffectState(BitUtil.getBlockState(placedBit));
                                            break;
                                        case FLUIDSTATE:
                                            inventory.setEffectState(BitUtil.getFluidState(placedBit).getBlockState());
                                            break;
                                        case COLOURED:
                                            inventory.setEffectState(Blocks.BLACK_CONCRETE.getDefaultState());
                                            break;
                                    }
                                    while (i.hasNext())
                                        if (inventory.placeBit(vb, i.x(), i.y(), i.z(), pkt.operation, placedBit) != 0) break;
                                }
                                break;
                                case REMOVE: {
                                    while (i.hasNext())
                                        if (inventory.removeBit(vb, i.x(), i.y(), i.z())) break;
                                    inventory.setEffectState(inventory.getMostCommonState());
                                }
                                break;
                            }

                            //Actually apply the operation.
                            tec.completeOperation(player, vb, true);

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
        if (it.getItem() instanceof RotatableItem)
            ((RotatableItem) it.getItem()).rotate(it, pkt.getAxis(), pkt.isClockwise());
        else {
            it = player.getHeldItemOffhand();
            if (it.getItem() instanceof RotatableItem)
                ((RotatableItem) it.getItem()).rotate(it, pkt.getAxis(), pkt.isClockwise());
        }
    }

    /**
     * Handles an incoming {@link CPlaceBlockPacket} packet.
     */
    public static void handle(final CPlaceBlockPacket pkt, final ServerPlayerEntity player) {
        final ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof VoxelStorer) || !(stack.getItem() instanceof BitModifyItem))
            return;

        if (!((BitModifyItem) stack.getItem()).canPerformModification(BitModifyItem.ModificationType.PLACE))
            return; //Make sure this item can do the operations

        final VoxelStorer it = (VoxelStorer) stack.getItem();
        final World world = player.world;
        final PlayerItemMode mode = player.getCapability(PlayerItemModeCapabilityProvider.PIMM).map(PlayerItemModeManager::getChiseledBlockMode).orElse((PlayerItemMode) ItemModeTypes.CHISELED_BLOCK.getDefault());
        final Direction face = pkt.side;
        final VoxelBlob blob = VoxelNBTConverter.readFromNBT(stack.getChildTag(VoxelNBTConverter.NBT_BLOCKENTITYTAG));

        //Check if we can place it here
        BlockPos actualPos = pkt.pos; //Placement block for non-offgird placement
        boolean canPlace = true;
        if (player.isCrouching() && !ClientItemPropertyUtil.getChiseledBlockMode().equals(PlayerItemMode.CHISELED_BLOCK_GRID)) {
            if (BlockPlacementLogic.isNotPlaceableOffGrid(player, player.world, face, pkt.location, player.getHeldItemMainhand()))
                canPlace = false;
        } else {
            if ((!ChiselUtil.isBlockReplaceable(player.world, actualPos, player, face, false) && ClientItemPropertyUtil.getChiseledBlockMode() == PlayerItemMode.CHISELED_BLOCK_GRID) || (!(player.world.getTileEntity(actualPos) instanceof ChiseledBlockTileEntity) && BlockPlacementLogic.isNotPlaceable(player, player.world, actualPos, face, mode, () -> blob)))
                actualPos = actualPos.offset(face);

            if (BlockPlacementLogic.isNotPlaceable(player, player.world, actualPos, face, mode, () -> blob))
                canPlace = false;
        }
        if (!canPlace) {
            player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.not_placeable"), true);
            return;
        }

        if (player.isCrouching()) {
            //Offgrid mode, place in all blockpositions concerned
            final ExtendedVoxelBlob evb = new ExtendedVoxelBlob(3, 3, 3, -1, -1, -1);
            final VoxelBlob placedBlob = it.getVoxelBlob(stack);
            evb.insertBlob(0, 0, 0, placedBlob);
            final IntegerBox bounds = placedBlob.getBounds();
            final BlockPos partialOffset = BlockPlacementLogic.getPartialOffset(pkt.side, new BlockPos(pkt.location.bitX, pkt.location.bitY, pkt.location.bitZ), bounds);
            evb.shift(partialOffset.getX(), partialOffset.getY(), partialOffset.getZ());

            for (BlockPos pos : evb.listBlocks()) {
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

                    switch (mode) {
                        case CHISELED_BLOCK_FIT:
                        case CHISELED_BLOCK_MERGE:
                        case CHISELED_BLOCK_OVERLAP:
                            final VoxelBlob vb = tec.getVoxelBlob();
                            if (mode.equals(PlayerItemMode.CHISELED_BLOCK_OVERLAP))
                                vb.overlap(slice);
                            else
                                vb.merge(slice);

                            tec.completeOperation(player, vb, false);
                            break;
                    }
                }
            }

            if (!player.isCreative())
                stack.setCount(stack.getCount() - 1);
            ChiselUtil.playModificationSound(world, pkt.location.blockPos, true); //Placement can play sound normally as block should be set already.
        } else {
            //Normal mode, place in this block position
            ChiselUtil.replaceWithChiseled(player.world, actualPos, player, world.getBlockState(actualPos), pkt.side);

            final TileEntity te = world.getTileEntity(actualPos);
            if (te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;
                switch (mode) {
                    case CHISELED_BLOCK_GRID:
                        tec.updateState(it.getVoxelBlob(stack));
                        break;
                    case CHISELED_BLOCK_FIT:
                    case CHISELED_BLOCK_MERGE:
                    case CHISELED_BLOCK_OVERLAP:
                        final VoxelBlob vb = tec.getVoxelBlob();
                        if (mode.equals(PlayerItemMode.CHISELED_BLOCK_OVERLAP))
                            vb.overlap(it.getVoxelBlob(stack));
                        else
                            vb.merge(it.getVoxelBlob(stack));
                        tec.completeOperation(player, vb, false);
                        break;
                }
                if (!player.isCreative())
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
        if (!(stack.getItem() instanceof StandardTypedItem) && !(stack.getItem() instanceof BitModifyItem))
            return;

        final StandardTypedItem tit = (StandardTypedItem) stack.getItem();
        if (!((BitModifyItem) tit).canPerformModification(BitModifyItem.ModificationType.ROTATE) && !((BitModifyItem) tit).canPerformModification(BitModifyItem.ModificationType.MIRROR))
            return; //Make sure this item can do the operations

        final World world = player.world;
        final BlockPos pos = pkt.pos;
        final BlockState state = world.getBlockState(pos);
        final Direction face = pkt.side;
        final ItemMode mode = tit.getSelectedMode(stack);

        if (!ChiselUtil.canChiselPosition(pos, player, state, face)) return;

        if (!(mode instanceof ItemModes))
            return; //We don't accept other modes for rotation.

        if (mode.equals(ItemModes.WRENCH_MIRROR)) {
            if (!RotationUtil.hasMirrorableState(state)) {
                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.not_mirrorable"), true);
                return;
            }
        } else if (mode.equals(ItemModes.WRENCH_ROTATE) || mode.equals(ItemModes.WRENCH_ROTATECCW)) {
            if (!RotationUtil.hasRotatableState(state)) {
                player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.not_rotatable"), true);
                return;
            }
        }

        //Custom chiseled block
        if (state.getBlock() instanceof ChiseledBlock) {
            final TileEntity te = world.getTileEntity(pos);
            if (te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity cte = (ChiseledBlockTileEntity) te;
                VoxelBlob voxel = cte.getVoxelBlob();
                if (mode.equals(ItemModes.WRENCH_MIRROR)) voxel = voxel.mirror(pkt.side.getAxis());
                else if (mode.equals(ItemModes.WRENCH_ROTATE)) voxel = voxel.spin(pkt.side.getAxis());
                else if (mode.equals(ItemModes.WRENCH_ROTATECCW)) voxel = voxel.spinCCW(pkt.side.getAxis());
                cte.updateState(voxel);
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

            if (mode.equals(ItemModes.WRENCH_MIRROR)) world.setBlockState(pos, state.mirror(RotationUtil.getMirror(pkt.side.getAxis())));
            else world.setBlockState(pos, state.rotate(world, pos, mode.equals(ItemModes.WRENCH_ROTATECCW) ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90));
        }

        //Reduce wrench durability
        player.getHeldItemMainhand().damageItem(1, player, (p) -> p.sendBreakAnimation(Hand.MAIN_HAND));
        player.getStats().increment(player, Stats.ITEM_USED.get(ChiselsAndBits2.getInstance().getRegister().WRENCH.get()), 1);
        ChiselUtil.playModificationSound(world, pos, true); //Wrench can play sound of the block as it has been set for this world.
    }
}
