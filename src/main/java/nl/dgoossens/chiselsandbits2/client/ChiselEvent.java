package nl.dgoossens.chiselsandbits2.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.item.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.impl.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.MenuAction;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.network.client.CWrenchBlockPacket;
import nl.dgoossens.chiselsandbits2.common.utils.ItemModeUtil;
import nl.dgoossens.chiselsandbits2.common.network.client.CChiselBlockPacket;
import nl.dgoossens.chiselsandbits2.common.utils.ChiselUtil;
import nl.dgoossens.chiselsandbits2.common.utils.RotationUtil;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChiselEvent {
    /**
     * We track the last time the player clicked to chisel to determine when 300ms have passed before
     * we allow another click. This event is client-sided so we only need a single variable.
     */
    private static long lastClick = System.currentTimeMillis();

    /*@SubscribeEvent
    public static void onClick(InputEvent.ClickInputEvent e) {
        if(e.isPickBlock()) {
            //If we're middle clicking and the keybind for selectbittype wasn't changed we cancel it and do our own implementation.
            if(ChiselsAndBits2.getKeybindings().selectBitType.getKey().equals(ChiselsAndBits2.getKeybindings().selectBitType.getDefault()))
                e.setCancelled(true);
            //Do pick bit
            return;
        }

    }*/

    /**
     * Will be removed when the proper ClickInputEvent gets added so we don't have to deal with
     * stupid minecraft bugs and de-syncs.
     */
    @SubscribeEvent
    public static void temporaryClick(PlayerInteractEvent e) {
        boolean leftClick = e instanceof PlayerInteractEvent.LeftClickBlock;
        if (!leftClick && !(e instanceof PlayerInteractEvent.RightClickBlock)) return;

        final PlayerEntity player = Minecraft.getInstance().player;
        RayTraceResult rtr = ChiselUtil.rayTrace(player);
        if (!(rtr instanceof BlockRayTraceResult) || rtr.getType() != RayTraceResult.Type.BLOCK) return;

        ItemStack i = player.getHeldItemMainhand();
        if(i.getItem() instanceof IBitModifyItem) {
            IBitModifyItem it = (IBitModifyItem) i.getItem();
            for(IBitModifyItem.ModificationType modificationType : IBitModifyItem.ModificationType.values()) {
                if(it.canPerformModification(modificationType) && it.validateUsedButton(modificationType, leftClick, i)) {
                    e.setCanceled(true);

                    if (System.currentTimeMillis() - lastClick < 150) return;
                    lastClick = System.currentTimeMillis();

                    switch(modificationType) {
                        case BUILD:
                        case EXTRACT:
                            //Chisel
                            final BitOperation operation = leftClick ? BitOperation.REMOVE : (ItemModeUtil.getMenuActionMode(i).equals(MenuAction.SWAP) ? BitOperation.SWAP : BitOperation.PLACE);
                            startChiselingBlock((BlockRayTraceResult) rtr, ItemModeUtil.getItemMode(i), player, operation);
                            break;
                        case ROTATE:
                        case MIRROR:
                            //Wrench
                            performBlockRotation((BlockRayTraceResult) rtr, ItemModeUtil.getItemMode(i), player);
                            break;
                        case CUSTOM:
                            it.performCustomModification(leftClick, i);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Handle the block chiselling with the given bit operation.
     */
    public static void startChiselingBlock(final BlockRayTraceResult rayTrace, final IItemMode mode, final PlayerEntity player, final BitOperation operation) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block chiseling can only be started on the client-side.");

        final BitLocation location = new BitLocation(rayTrace, true, operation);
        final BlockPos pos = location.getBlockPos(); //We get the location from the bitlocation because that takes placement offset into account. (placement can go into neighbouring block)
        final BlockState state = player.world.getBlockState(pos);
        final Direction face = rayTrace.getFace();
        //We use a the constructor for BlockRayTraceResult from a method in BlockItemUseContext.
        BlockItemUseContext context = new BlockItemUseContext(new ItemUseContext(player, Hand.MAIN_HAND, new BlockRayTraceResult(new Vec3d((double) pos.getX() + 0.5D + (double) face.getXOffset() * 0.5D, (double) pos.getY() + 0.5D + (double) face.getYOffset() * 0.5D, (double) pos.getZ() + 0.5D + (double) face.getZOffset() * 0.5D), face, pos, false)));
        if (!state.isReplaceable(context) && !ChiselUtil.canChiselBlock(state)) return; //You can place on replacable blocks.
        if (!ChiselUtil.canChiselPosition(pos, player, state, rayTrace.getFace())) return;

        if(ItemModeUtil.getItemMode(player.getHeldItemMainhand()).equals(ItemMode.CHISEL_DRAWN_REGION)) {
            ClientSide clientSide = ChiselsAndBits2.getInstance().getClient();
            //If we don't have a selection start yet select the clicked location.
            if(!clientSide.hasSelectionStart(operation)) {
                clientSide.setSelectionStart(operation, location);
                return;
            }
        }

        //Default for remove operations, we only get a placed bit when not removing
        int placedBit = -1;
        if(!operation.equals(BitOperation.REMOVE))
            placedBit = ItemModeUtil.getGlobalSelectedItemMode(player).getPlacementBitId(context);
        //We determine the placed bit on the client and include it in the packet so we can reuse the BlockItemUseContext from earlier.

        //If we couldn't find a selected type, don't chisel.
        if (placedBit == VoxelBlob.AIR_BIT) {
            player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.no_selected_type"), true);
            return;
        }

        if(ItemMode.CHISEL_DRAWN_REGION.equals(ItemModeUtil.getItemMode(player.getHeldItemMainhand()))) {
            final CChiselBlockPacket pc = new CChiselBlockPacket(operation, ChiselsAndBits2.getInstance().getClient().getSelectionStart(operation), location, face, mode, placedBit);
            ChiselsAndBits2.getInstance().getClient().resetSelectionStart();
            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
        } else {
            final CChiselBlockPacket pc = new CChiselBlockPacket(operation, location, face, mode, placedBit);
            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
        }
    }

    /**
     * Handle the block being rotated.
     */
    public static void performBlockRotation(final BlockRayTraceResult rayTrace, final IItemMode mode, final PlayerEntity player) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block rotation can only be started on the client-side.");

        final BlockPos pos = rayTrace.getPos();
        final BlockState state = player.world.getBlockState(pos);
        final Direction face = rayTrace.getFace();

        if (!ChiselUtil.canChiselPosition(pos, player, state, rayTrace.getFace())) return;
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

        final CWrenchBlockPacket pc = new CWrenchBlockPacket(pos, face, mode);
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
    }
}
