package nl.dgoossens.chiselsandbits2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.common.items.ChiselMimicItem;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.network.client.CPlaceBlockPacket;
import nl.dgoossens.chiselsandbits2.common.network.client.CWrenchBlockPacket;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.network.client.CChiselBlockPacket;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChiselEvent {
    /**
     * We track the last time the player clicked to chisel to determine when 300ms have passed before
     * we allow another click. This event is client-sided so we only need a single variable.
     */
    private static long lastClick = System.currentTimeMillis();

    /**
     * Will be removed when the proper ClickInputEvent gets added so we don't have to deal with
     * stupid minecraft bugs and de-syncs.
     */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent e) {
        //--- Locking Morphing Bits ---
        if(e instanceof PlayerInteractEvent.RightClickEmpty || e instanceof PlayerInteractEvent.LeftClickEmpty) {
            if(e.getPlayer().getHeldItemMainhand().getItem() instanceof MorphingBitItem && e.getPlayer().isCrouching()) {
                if (System.currentTimeMillis() - lastClick < 150) return;
                lastClick = System.currentTimeMillis();

                final ItemStack i = e.getPlayer().getHeldItemMainhand();
                boolean b = ((MorphingBitItem) i.getItem()).isLocked(i);
                ClientItemPropertyUtil.setLockState(!b);
                return;
            }
        }

        //--- All other item interaction actions ---
        boolean leftClick = e instanceof PlayerInteractEvent.LeftClickBlock;
        if (!leftClick && !(e instanceof PlayerInteractEvent.RightClickBlock)) return;

        final PlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.getHeldItemMainhand();
        if(stack.getItem() instanceof IBitModifyItem) {
            IBitModifyItem it = (IBitModifyItem) stack.getItem();
            for(IBitModifyItem.ModificationType modificationType : IBitModifyItem.ModificationType.values()) {
                if(it.canPerformModification(modificationType) && it.validateUsedButton(modificationType, leftClick, stack)) {
                    e.setCanceled(true);

                    if (System.currentTimeMillis() - lastClick < 150) return;
                    lastClick = System.currentTimeMillis();

                    switch(modificationType) {
                        case BUILD:
                        case EXTRACT:
                            //Chisel
                            startChiselingBlock(leftClick, player, stack);
                            break;
                        case ROTATE:
                        case MIRROR:
                            //Wrench
                            performBlockRotation(player, stack);
                            break;
                        case PLACE:
                            //Chiseled Block
                            performPlaceBlock(player);
                            break;
                        case CUSTOM:
                            //Custom modification
                            it.performCustomModification(leftClick, stack);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Handle the block chiselling with the given bit operation.
     */
    public static void startChiselingBlock(final boolean leftClick, final PlayerEntity player, final ItemStack stack) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block chiseling can only be started on the client-side.");

        if(!(stack.getItem() instanceof ChiselMimicItem)) return;
        ChiselMimicItem tit = (ChiselMimicItem) stack.getItem();
        final IItemMode mode = tit.getSelectedMode(stack);

        RayTraceResult rayTrace = ChiselUtil.rayTrace(player);
        if (!(rayTrace instanceof BlockRayTraceResult) || rayTrace.getType() != RayTraceResult.Type.BLOCK)
            return;

        final BitOperation operation = leftClick ? BitOperation.REMOVE : (tit.isSwapping(stack) ? BitOperation.SWAP : BitOperation.PLACE);
        final BitLocation location = new BitLocation((BlockRayTraceResult) rayTrace, true, operation);

        //Start drawn region selection if applicable
        if(ItemPropertyUtil.isItemMode(player.getHeldItemMainhand(), ItemMode.CHISEL_DRAWN_REGION)) {
            ClientSide clientSide = ChiselsAndBits2.getInstance().getClient();
            //If we don't have a selection start yet select the clicked location.
            if(!clientSide.hasSelectionStart(operation)) {
                clientSide.setSelectionStart(operation, location);
                return;
            }
        }

        //Send correct packet depending on whether this is drawn region or not
        if(ItemPropertyUtil.isItemMode(player.getHeldItemMainhand(), ItemMode.CHISEL_DRAWN_REGION)) {
            final CChiselBlockPacket pc = new CChiselBlockPacket(operation, ChiselsAndBits2.getInstance().getClient().getSelectionStart(operation), location,  ((BlockRayTraceResult) rayTrace).getFace());
            ChiselsAndBits2.getInstance().getClient().resetSelectionStart();
            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
        } else if(mode instanceof ItemMode) {
            final CChiselBlockPacket pc = new CChiselBlockPacket(operation, location, ((BlockRayTraceResult) rayTrace).getFace());
            ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
        }
    }

    /**
     * Handles placing a chiseled block.
     */
    public static void performPlaceBlock(final PlayerEntity player) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block placement can only be started on the client-side.");

        final RayTraceResult rayTrace = ChiselUtil.rayTrace(player);
        if (!(rayTrace instanceof BlockRayTraceResult) || rayTrace.getType() != RayTraceResult.Type.BLOCK)
            return;

        final BitLocation bl = new BitLocation((BlockRayTraceResult) rayTrace, true, BitOperation.PLACE);
        final CPlaceBlockPacket pc = new CPlaceBlockPacket(((BlockRayTraceResult) rayTrace).getPos(), bl, ((BlockRayTraceResult) rayTrace).getFace());
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
    }

    /**
     * Handle the block being rotated.
     */
    public static void performBlockRotation(final PlayerEntity player, final ItemStack stack) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block rotation can only be started on the client-side.");

        final RayTraceResult rayTrace = Minecraft.getInstance().objectMouseOver;
        if(!(rayTrace instanceof BlockRayTraceResult) || rayTrace.getType() != RayTraceResult.Type.BLOCK)
            return;

        final CWrenchBlockPacket pc = new CWrenchBlockPacket(((BlockRayTraceResult) rayTrace).getPos(), ((BlockRayTraceResult) rayTrace).getFace());
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(pc);
    }
}
