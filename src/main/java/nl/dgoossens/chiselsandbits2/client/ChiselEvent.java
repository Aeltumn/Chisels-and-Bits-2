package nl.dgoossens.chiselsandbits2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
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
import nl.dgoossens.chiselsandbits2.common.items.TapeMeasureItem;
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
    public static void onPlayerInteract(InputEvent.ClickInputEvent e) {
        PlayerEntity player = Minecraft.getInstance().player;

        //--- Locking Morphing Bits ---
        if(e.isUseItem()) {
            if(player.getHeldItemMainhand().getItem() instanceof MorphingBitItem && player.isCrouching()) {
                if (System.currentTimeMillis() - lastClick < 150) return;
                lastClick = System.currentTimeMillis();

                final ItemStack i = player.getHeldItemMainhand();
                boolean b = ((MorphingBitItem) i.getItem()).isLocked(i);
                ClientItemPropertyUtil.setLockState(!b);
                return;
            }
        }

        //--- Pick Bit ---
        if(e.isPickBlock()) {
            //TODO pick bit!
            return;
        }

        //--- All other item interaction actions ---
        ItemStack stack = player.getHeldItemMainhand();
        if(stack.getItem() instanceof IBitModifyItem) {
            //Trigger an effect for one of our own tools.
            //We do it here instead of using Minecraft's method to cancel it properly for invalid blocks. (also to use our own raytracing)
            IBitModifyItem it = (IBitModifyItem) stack.getItem();
            for(IBitModifyItem.ModificationType modificationType : IBitModifyItem.ModificationType.values()) {
                if(it.canPerformModification(modificationType) && it.validateUsedButton(modificationType, e.isAttack(), stack)) {
                    e.setSwingHand(true);
                    e.setCanceled(true);

                    if (System.currentTimeMillis() - lastClick < 150) return;
                    lastClick = System.currentTimeMillis();

                    switch(modificationType) {
                        case BUILD:
                        case EXTRACT:
                            //Chisel
                            startChiselingBlock(e.isAttack(), player, stack);
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
                            it.performCustomModification(e.isAttack(), stack);
                            break;
                    }
                }
            }
        } else if(stack.getItem() instanceof TapeMeasureItem && e.isUseItem()) {
            //We trigger the tape measure here as it works even if we don't hit a block. If we used `useItem` in Item.class it would only trigger when hitting a block.
            e.setSwingHand(true);
            e.setCanceled(true);

            //Clear measurements if there are measurements and we're not currently selecting one.
            if(ChiselsAndBits2.getInstance().getClient().tapeMeasureCache == null && player.isCrouching() && !ChiselsAndBits2.getInstance().getClient().tapeMeasurements.isEmpty()) {
                ChiselsAndBits2.getInstance().getClient().tapeMeasurements.clear();
                player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.cleared_measurements"), true);
                return;
            }

            RayTraceResult rayTrace = ChiselUtil.rayTrace(player);
            if (!(rayTrace instanceof BlockRayTraceResult) || rayTrace.getType() != RayTraceResult.Type.BLOCK)
                return;

            ChiselsAndBits2.getInstance().getClient().useTapeMeasure((BlockRayTraceResult) rayTrace);
        }
    }

    /**
     * Handle the block chiselling with the given bit operation.
     */
    public static void startChiselingBlock(final boolean attack, final PlayerEntity player, final ItemStack stack) {
        if (!player.world.isRemote)
            throw new UnsupportedOperationException("Block chiseling can only be started on the client-side.");

        if(!(stack.getItem() instanceof ChiselMimicItem)) return;
        ChiselMimicItem tit = (ChiselMimicItem) stack.getItem();
        final IItemMode mode = tit.getSelectedMode(stack);

        RayTraceResult rayTrace = ChiselUtil.rayTrace(player);
        if (!(rayTrace instanceof BlockRayTraceResult) || rayTrace.getType() != RayTraceResult.Type.BLOCK)
            return;

        final BitOperation operation = attack ? BitOperation.REMOVE : (tit.isSwapping(stack) ? BitOperation.SWAP : BitOperation.PLACE);
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
