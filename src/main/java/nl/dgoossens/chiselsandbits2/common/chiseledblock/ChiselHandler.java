package nl.dgoossens.chiselsandbits2.common.chiseledblock;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IRotatableItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IVoxelStorer;
import nl.dgoossens.chiselsandbits2.client.BlockPlacementLogic;
import nl.dgoossens.chiselsandbits2.client.cull.DummyEnvironmentWorldReader;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel.ChiselIterator;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.VoxelRegionSrc;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.network.client.CPlaceBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CWrenchBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CRotateItemPacket;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
import nl.dgoossens.chiselsandbits2.common.network.client.CChiselBlockPacket;
import nl.dgoossens.chiselsandbits2.common.util.InventoryUtils;
import nl.dgoossens.chiselsandbits2.common.util.RotationUtil;

import static nl.dgoossens.chiselsandbits2.api.block.BitOperation.*;

/**
 * Handles incoming packets that relate to interacting with voxelblobs.
 */
public class ChiselHandler {
    /**
     * Handles an incoming {@link CChiselBlockPacket} packet.
     */
    public static void handle(final CChiselBlockPacket pkt, final ServerPlayerEntity player) {
        if (!(player.getHeldItemMainhand().getItem() instanceof IBitModifyItem))
            return; //Extra security, if you're somehow no longer holding a valid item that can chisel we cancel.
        IBitModifyItem ibm = (IBitModifyItem) player.getHeldItemMainhand().getItem();
        if(!ibm.canPerformModification(IBitModifyItem.ModificationType.BUILD) && !ibm.canPerformModification(IBitModifyItem.ModificationType.EXTRACT))
            return; //Make sure this item can do the operations

        final World world = player.world;
        final InventoryUtils.CalculatedInventory inventory = InventoryUtils.buildInventory(player);

        //Cancel chiseling early on if possible (but not if creative)
        if (inventory.getAvailableDurability() <= 0 && !player.isCreative()) {
            player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.need_chisel"), true);
            return;
        }

        final VoxelWrapper wrapper = VoxelWrapper.forAbstract(pkt.placedBit);

        //Placement/swap which means we need to track extracted bits
        if(!pkt.operation.equals(REMOVE)) {
            inventory.trackMaterialUsage(wrapper);

            //Cancel chiseling early on if possible (but not if creative)
            //This shouldn't ever happen because you can't have a material selected that you don't have.
            //Well actually this can happen if you drop the bags and instantly click before the routine updating catches up to you. tl;dr this can only happen if the cached value isn't updated on time
            if (inventory.getAvailableMaterial() <= 0 && !player.isCreative()) {
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.need_bits"), true);
                return;
            }
        }

        //If world.getServer() is null we return to make the "if (world.getServer().isBlockProtected(world, pos, player))" never fail.
        if (world.getServer() == null)
            return;

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
                        //This method is specifically a server only method, the client already does checking if we can chisel somewhere through ChiselUtil#canChiselPosition.
                        if (world.getServer().isBlockProtected(world, pos, player))
                            continue;

                        //Replace the block with a chiseled block.
                        ChiselUtil.replaceWithChiseled(player, world, pos, world.getBlockState(pos), pkt.side);

                        final TileEntity te = world.getTileEntity(pos);
                        if (te instanceof ChiseledBlockTileEntity) {
                            final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;
                            final VoxelBlob vb = tec.getVoxelBlob();
                            final ChiselIterator i = ChiselUtil.getIterator(pkt, new VoxelRegionSrc(player, world, pos, 1), pos, pkt.operation);

                            //Handle the operation
                            switch (pkt.operation) {
                                case SWAP:
                                case PLACE: {
                                    inventory.setEffectState(BitUtil.getBlockState(pkt.placedBit)); //TODO add support for fluids/coloured bits
                                    while (i.hasNext())
                                        if(inventory.placeBit(vb, i.x(), i.y(), i.z(), pkt.operation, pkt.placedBit) != 0) break;
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

    /**
     * Handles an incoming {@link CRotateItemPacket} packet.
     */
    public static void handle(final CRotateItemPacket pkt, final ServerPlayerEntity player) {
        //Try to rotate both hands if possible
        ItemStack it = player.getHeldItemMainhand();
        if(it.getItem() instanceof IRotatableItem)
            ((IRotatableItem) it.getItem()).rotate(it, pkt.getAxis());
        else {
            it = player.getHeldItemOffhand();
            if(it.getItem() instanceof IRotatableItem)
                ((IRotatableItem) it.getItem()).rotate(it, pkt.getAxis());
        }
    }

    /**
     * Handles an incoming {@link CPlaceBlockPacket} packet.
     */
    public static void handle(final CPlaceBlockPacket pkt, final ServerPlayerEntity player) {
        if (!(player.getHeldItemMainhand().getItem() instanceof IBitModifyItem))
            return; //Extra security, if you're somehow no longer holding a valid item that can chisel we cancel.
        IBitModifyItem ibm = (IBitModifyItem) player.getHeldItemMainhand().getItem();
        if (!ibm.canPerformModification(IBitModifyItem.ModificationType.PLACE))
            return; //Make sure this item can do the operations

        final World world = player.world;

        if(pkt.mode instanceof ItemMode) {
            ItemStack item = player.getHeldItemMainhand();
            if(!(item.getItem() instanceof IVoxelStorer)) return;
            IVoxelStorer it = (IVoxelStorer) item.getItem();

            if(pkt.offgrid) {
                //Offgrid mode, place in all blockpositions concerned
                final BitLocation target = pkt.location;
                final VoxelBlob placedBlob = it.getVoxelBlob(item);
                final IntegerBox bounds = placedBlob.getBounds();
                final BlockPos offset = BlockPlacementLogic.getPartialOffset(pkt.side, new BlockPos(target.bitX, target.bitY, target.bitZ), bounds);
                System.out.println("Offset is "+offset);
                final BitLocation offsetLocation = new BitLocation(target).add(offset.getX(), offset.getY(), offset.getZ());
                final BlockPos from = offsetLocation.getBlockPos();
                final BlockPos to = offsetLocation.add(bounds.width(), bounds.height(), bounds.depth()).getBlockPos();
                System.out.println("Placing from "+from+" to "+to);

                final int maxX = Math.max(from.getX(), to.getX());
                final int maxY = Math.max(from.getY(), to.getY());
                final int maxZ = Math.max(from.getZ(), to.getZ());
                for (int xOff = Math.min(from.getX(), to.getX()); xOff <= maxX; ++xOff) {
                    for (int yOff = Math.min(from.getY(), to.getY()); yOff <= maxY; ++yOff) {
                        for (int zOff = Math.min(from.getZ(), to.getZ()); zOff <= maxZ; ++zOff) {
                            final BlockPos pos = new BlockPos(xOff, yOff, zOff);
                            //If we can't chisel here, don't chisel.
                            if (world.getServer().isBlockProtected(world, pos, player))
                                continue;
                            if (!ChiselUtil.canChiselPosition(pos, player, world.getBlockState(pos), pkt.side))
                                continue;

                            final int myX = (xOff - target.blockPos.getX()) * 16 - target.bitX;
                            final int myY = yOff * 16 - target.bitY - target.blockPos.getY() * 16;
                            final int myZ = zOff * 16 - target.bitZ - target.blockPos.getZ() * 16;
                            final VoxelBlob slice = placedBlob.offset(myX, myY, myZ); //Calculate what part of the block needs to go onto this block

                            //Replace the block with a chiseled block.
                            ChiselUtil.replaceWithChiseled(player, world, pos, world.getBlockState(pos), pkt.side);

                            final TileEntity te = world.getTileEntity(pos);
                            if (te instanceof ChiseledBlockTileEntity) {
                                final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;

                                switch(pkt.mode) {
                                    case CHISELED_BLOCK_FIT:
                                    case CHISELED_BLOCK_MERGE:
                                    case CHISELED_BLOCK_OVERLAP:
                                        final VoxelBlob vb = tec.getVoxelBlob();
                                        if(pkt.mode.equals(ItemMode.CHISELED_BLOCK_OVERLAP))
                                            vb.overlap(slice);
                                        else
                                            vb.merge(slice);
                                        tec.completeEditOperation(player, vb, false);
                                        break;
                                }
                                if(!player.isCreative())
                                    item.setCount(item.getCount() - 1);
                                ChiselUtil.playModificationSound(world, pos, true); //Placement can play sound normally as block should be set already.
                            }
                        }
                    }
                }
                return;
            }

            //Normal mode, place in this block position
            final BlockPos pos = pkt.pos;
            ChiselUtil.replaceWithChiseled(player, player.world, pos, world.getBlockState(pos), pkt.side);

            final TileEntity te = world.getTileEntity(pos);
            if (te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity tec = (ChiseledBlockTileEntity) te;
                switch(pkt.mode) {
                    case CHISELED_BLOCK_GRID:
                        tec.setBlob(it.getVoxelBlob(item));
                        break;
                    case CHISELED_BLOCK_FIT:
                    case CHISELED_BLOCK_MERGE:
                    case CHISELED_BLOCK_OVERLAP:
                        final VoxelBlob vb = tec.getVoxelBlob();
                        if(pkt.mode.equals(ItemMode.CHISELED_BLOCK_OVERLAP))
                            vb.overlap(it.getVoxelBlob(item));
                        else
                            vb.merge(it.getVoxelBlob(item));
                        tec.completeEditOperation(player, vb, false);
                        break;
                }
                if(!player.isCreative())
                    item.setCount(item.getCount() - 1);
                ChiselUtil.playModificationSound(world, pos, true); //Placement can play sound normally as block should be set already.
            }
        }
    }

    /**
     * Handles an incoming {@link CWrenchBlockPacket} packet.
     */
    public static void handle(final CWrenchBlockPacket pkt, final ServerPlayerEntity player) {
        if (!(player.getHeldItemMainhand().getItem() instanceof IBitModifyItem))
            return; //Extra security, if you're somehow no longer holding a valid item that can chisel we cancel.
        IBitModifyItem ibm = (IBitModifyItem) player.getHeldItemMainhand().getItem();
        if(!ibm.canPerformModification(IBitModifyItem.ModificationType.ROTATE) && !ibm.canPerformModification(IBitModifyItem.ModificationType.MIRROR))
            return; //Make sure this item can do the operations

        final World world = player.world;
        final BlockPos pos = pkt.pos;
        final BlockState state = world.getBlockState(pos);

        //Security check if state is still valid
        if(pkt.mode.equals(ItemMode.WRENCH_MIRROR) ? !RotationUtil.hasMirrorableState(state) : !RotationUtil.hasRotatableState(state))
            return;

        //Custom chiseled block
        if(state.getBlock() instanceof ChiseledBlock) {
            final TileEntity te = world.getTileEntity(pos);
            if(te instanceof ChiseledBlockTileEntity) {
                final ChiseledBlockTileEntity cte = (ChiseledBlockTileEntity) te;
                VoxelBlob voxel = cte.getVoxelBlob();
                if(pkt.mode.equals(ItemMode.WRENCH_MIRROR)) voxel = voxel.mirror(pkt.side.getAxis());
                else if(pkt.mode.equals(ItemMode.WRENCH_ROTATE)) voxel = voxel.spin(pkt.side.getAxis());
                else if(pkt.mode.equals(ItemMode.WRENCH_ROTATECCW)) voxel = voxel.spinCCW(pkt.side.getAxis());
                cte.setBlob(voxel);
            }
        } else {
            //Other rotatable blocks
            DummyEnvironmentWorldReader dummyWorld = new DummyEnvironmentWorldReader() {
                @Override
                public BlockState getBlockState(BlockPos pos) {
                    if (pos.equals(BlockPos.ZERO)) return state;
                    return super.getBlockState(pos);
                }
            };
            if (state.getBlockHardness(dummyWorld, BlockPos.ZERO) < 0) return; //Can't move unbreakable blocks. (they have -1 hardness)

            if(pkt.mode.equals(ItemMode.WRENCH_MIRROR)) world.setBlockState(pos, state.mirror(RotationUtil.getMirror(pkt.side.getAxis())));
            else world.setBlockState(pos, state.rotate(world, pos, pkt.mode.equals(ItemMode.WRENCH_ROTATECCW) ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90));
        }

        //Reduce wrench durability
        player.getHeldItemMainhand().damageItem(1, player, (p) -> p.sendBreakAnimation(Hand.MAIN_HAND));
        player.getStats().increment(player, Stats.ITEM_USED.get(ChiselsAndBits2.getInstance().getItems().WRENCH), 1);
        ChiselUtil.playModificationSound(world, pos, true); //Wrench can play sound of the block as it has been set for this world.
    }
}
