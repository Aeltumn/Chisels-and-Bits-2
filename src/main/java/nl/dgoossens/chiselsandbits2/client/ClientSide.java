package nl.dgoossens.chiselsandbits2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.cache.CacheType;
import nl.dgoossens.chiselsandbits2.api.item.*;
import nl.dgoossens.chiselsandbits2.api.item.IMenuAction;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IItemScrollWheel;
import nl.dgoossens.chiselsandbits2.client.render.color.ColourableItemColor;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledBlockColor;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledBlockItemColor;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.MenuAction;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.items.ChiselMimicItem;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.registry.ModKeybindings;

import java.lang.reflect.Field;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.FOOD;

/**
 * Handles all features triggered by client-sided events.
 * Examples:
 * - Block Highlights
 * - Placement Ghost
 * - Tape Measure
 * - Item Scrolling
 *
 * Events are located in this class, all methods are put in ClientSideHelper.
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientSide extends ClientSideHelper {
    //--- GENERAL SETUP ---
    /**
     * Register listeners to mod event bus.
     */
    public void initialise() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerItemColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerBlockColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerIconTextures);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clearCaches);
    }

    /**
     * Register item color handlers.
     */
    private void registerItemColors(final ColorHandlerEvent.Item e) {
        Registration m = ChiselsAndBits2.getInstance().getRegister();
        
        //Register all items with an item color
        e.getItemColors().register(new ChiseledBlockItemColor(),
                m.CHISELED_BLOCK_ITEM.get(),
                m.MORPHING_BIT.get());

        e.getItemColors().register(new ColourableItemColor(1),
                m.WHITE_BIT_BAG.get(),
                m.ORANGE_BIT_BAG.get(),
                m.BLACK_BIT_BAG.get(),
                m.BLUE_BIT_BAG.get(),
                m.LIGHT_BLUE_BIT_BAG.get(),
                m.LIGHT_GRAY_BIT_BAG.get(),
                m.BROWN_BIT_BAG.get(),
                m.CYAN_BIT_BAG.get(),
                m.RED_BIT_BAG.get(),
                m.YELLOW_BIT_BAG.get(),
                m.PINK_BIT_BAG.get(),
                m.GRAY_BIT_BAG.get(),
                m.PURPLE_BIT_BAG.get(),
                m.LIME_BIT_BAG.get(),
                m.MAGENTA_BIT_BAG.get(),
                m.GREEN_BIT_BAG.get());
    }

    /**
     * Register block color handlers.
     */
    private void registerBlockColors(final ColorHandlerEvent.Block e) {
        e.getBlockColors().register(new ChiseledBlockColor(), ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get());
    }

    /**
     * Register custom sprites.
     */
    private void registerIconTextures(final TextureStitchEvent.Pre e) {
        //Only register to the texture map.
        if(!e.getMap().getTextureLocation().equals(PlayerContainer.LOCATION_BLOCKS_TEXTURE))
            return;

        //We only do this for our own, addons need to do this themselves.
        for (final IMenuAction menuAction : MenuAction.values()) {
            if (!menuAction.hasIcon()) continue;
            menuActionLocations.put(menuAction, menuAction.getIconResourceLocation());
            e.addSprite(menuActionLocations.get(menuAction));
        }

        //Register icons for item modes
        for (final ItemModeEnum itemMode : ItemMode.values())
            addItemModeSprite(e, itemMode);
        for (final ItemModeEnum itemMode : PlayerItemMode.values())
            addItemModeSprite(e, itemMode);
    }

    //Utility method to make it easier to add item mode sprites
    private void addItemModeSprite(final TextureStitchEvent.Pre e, final ItemModeEnum itemMode) {
        if (!itemMode.hasIcon()) return;
        modeIconLocations.put(itemMode, itemMode.getIconResourceLocation());
        e.addSprite(modeIconLocations.get(itemMode));
    }

    /**
     * Clear the cached model data whenever textures are stitched.
     */
    private void clearCaches(final TextureStitchEvent.Post e) {
        CacheType.DEFAULT.call();
    }

    /**
     * Call the clean method.
     */
    @SubscribeEvent
    public static void cleanupOnQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        ChiselsAndBits2.getInstance().getClient().clean();
    }

    /**
     * Handles hotkey presses.
     */
    @SubscribeEvent
    public static void onKeyInput(final InputEvent.KeyInputEvent e) {
        //Return if not in game.
        if (Minecraft.getInstance().player == null) return;

        final ModKeybindings keybindings = ChiselsAndBits2.getInstance().getKeybindings();
        for (ItemModeEnum im : keybindings.modeHotkeys.keySet()) {
            KeyBinding kb = keybindings.modeHotkeys.get(im);
            if (kb.isPressed() && kb.getKeyModifier().isActive(KeyConflictContext.IN_GAME))
                ItemPropertyUtil.setItemMode(Minecraft.getInstance().player, Minecraft.getInstance().player.getHeldItemMainhand(), im);
        }
        for (IMenuAction ma : keybindings.actionHotkeys.keySet()) {
            KeyBinding kb = keybindings.actionHotkeys.get(ma);
            if (kb.isPressed() && kb.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
                if(ma.equals(MenuAction.PLACE) || ma.equals(MenuAction.SWAP)) {
                    ItemStack stack = Minecraft.getInstance().player.getHeldItemMainhand();
                    if(stack.getItem() instanceof ChiselMimicItem) {
                        if (((ChiselMimicItem) stack.getItem()).isPlacing(stack))
                            MenuAction.PLACE.trigger();
                        else
                             MenuAction.SWAP.trigger();
                    }
                    continue;
                }
                ma.trigger();
            }
        }
    }


    /**
     * For rendering the preview selected menu option on the
     * item portrait.
     */
    @SubscribeEvent
    public static void drawLast(final RenderGameOverlayEvent.Post e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && ChiselsAndBits2.getInstance().getConfig().enableToolbarIcons.get()) {
            final PlayerEntity player = Minecraft.getInstance().player;
            if (player != null && !player.isSpectator() && hasToolbarIconItem(player.inventory))
                ChiselsAndBits2.getInstance().getClient().renderSelectedModePreviews(e.getWindow());
        }
    }

    /**
     * For drawing our custom highlight bounding boxes!
     */
    @SubscribeEvent
    public static void drawHighlights(final DrawHighlightEvent.HighlightBlock e) {
        //Cancel if the draw blocks highlight method successfully rendered a highlight.
        if(ChiselsAndBits2.getInstance().getClient().drawBlockHighlight(e.getMatrix(), e.getBuffers(), e.getPartialTicks()))
            e.setCanceled(true);
    }

    /**
     * For rendering the block placement ghost and static tape measurements.
     */
    @SubscribeEvent
    public static void drawLast(final RenderWorldLastEvent e) {
        if (Minecraft.getInstance().gameSettings.hideGUI) return;

        ClientSide client = ChiselsAndBits2.getInstance().getClient();
        client.renderTapeMeasureBoxes(e.getPartialTicks());
        client.renderPlacementGhost(e.getPartialTicks());
    }

    /**
     * Handles calling the scroll methods on all items implementing IItemScrollWheel.
     */
    @SubscribeEvent
    public static void wheelEvent(final InputEvent.MouseScrollEvent me) {
        final int dwheel = me.getScrollDelta() < 0 ? -1 : me.getScrollDelta() > 0 ? 1 : 0;
        if (me.isCanceled() || dwheel == 0) return;

        final PlayerEntity player = Minecraft.getInstance().player;
        final ItemStack is = player.getHeldItemMainhand();

        if (is.getItem() instanceof IItemScrollWheel && player.isCrouching()) {
            if(((IItemScrollWheel) is.getItem()).scroll(player, is, dwheel))
                me.setCanceled(true);
        }
    }
}
