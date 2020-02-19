package nl.dgoossens.chiselsandbits2.api.radial;

import com.google.common.base.Stopwatch;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IItemMenu;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.TimeUnit;

/**
 * From Chisels & Bits by AlgorithmX2
 * Rewritten into abstract by Aeltumn
 *
 * Note: this is still a work in progress and the rendering parts will be abstracted too in the future
 */
public abstract class RadialMenu extends Screen {
    //--- RADIAL MENU INSTANCE ---
    public static final RadialMenu RADIAL_MENU = null;//new ItemModeMenu(); //Change this to your own class

    //--- GETTERS FOR INSTANCES ---
    private Minecraft minecraft;
    private FontRenderer fontRenderer;

    public RadialMenu(ITextComponent name) {
        super(name);
    }

    /**
     * Get the Minecraft instance to use in this screen.
     */
    @Override
    public Minecraft getMinecraft() {
        if(minecraft == null) minecraft = Minecraft.getInstance();
        return minecraft;
    }

    /**
     * Get the main font renderer that can be used to render texts.
     */
    public FontRenderer getFontRenderer() {
        if(fontRenderer == null) fontRenderer = getMinecraft().fontRenderer;
        return fontRenderer;
    }

    //--- LOGIC METHODS ---
    public final static float TIME_SCALE = 0.01f;
    private float visibility = 0.0f;
    private Stopwatch lastChange = Stopwatch.createStarted();
    private long lastSelection = 0, resetMenu = 0;
    private boolean currentlyShown = false, pressedButton = false, effectHappened = false;
    private MainWindow window;
    private Runnable action = null;

    public void cleanup() {
        //Reset everything to avoid menu destroying itself
        visibility = 0.0f;
        lastChange = Stopwatch.createStarted();
        lastSelection = 0;
        currentlyShown = false;
        pressedButton = false;
        effectHappened = false;
        resetMenu = System.currentTimeMillis();
    }

    protected boolean inTriangle(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3, final double x, final double y) {
        final double ab = (x1 - x) * (y2 - y) - (x2 - x) * (y1 - y);
        final double bc = (x2 - x) * (y3 - y) - (x3 - x) * (y2 - y);
        final double ca = (x3 - x) * (y1 - y) - (x1 - x) * (y3 - y);
        return sign(ab) == sign(bc) && sign(bc) == sign(ca);
    }

    protected int sign(final double n) {
        return n > 0 ? 1 : -1;
    }

    /**
     * Clamps a float between 0.0f and 1.0f.
     */
    protected float clampFloat(final float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    /**
     * Raises the visiblity of the menu.
     */
    public void raiseVisibility() {
        visibility = clampFloat(visibility + lastChange.elapsed(TimeUnit.MILLISECONDS) * TIME_SCALE);
        lastChange = Stopwatch.createStarted();
    }

    /**
     * Decreases the visiblity of the menu.
     */
    public void decreaseVisibility() {
        visibility = clampFloat(visibility - lastChange.elapsed(TimeUnit.MILLISECONDS) * TIME_SCALE);
        lastChange = Stopwatch.createStarted();
    }

    /**
     * Returns whether or not the radial menu is currently visible.
     */
    public boolean isVisible() {
        return visibility > 0.001;
    }

    public float getVisibility() {
        return visibility;
    }

    public MainWindow getWindow() {
        return window;
    }

    public void configure(final MainWindow window) {
        this.window = window;
    }

    public boolean isPressingButton() {
        return pressedButton;
    }

    public void setPressingButton(boolean d) {
        pressedButton = d;
    }

    public boolean hasAction() {
        return action != null;
    }

    public Runnable getAction() {
        return action;
    }

    public void resetAction() {
        action = null;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    public void selectHoverOver() {
        if (hasSelection()) {
            lastSelection = System.currentTimeMillis();
            effectHappened = true;
            triggerEffect();
        }
    }

    /**
     * A main tick method  that handles if the button is pressed and updating the visibility.
     */
    public void tick() {
        PlayerEntity player = Minecraft.getInstance().player;
        updateGameFocus(); //(un)grab mouse and set current screen if it doesn't match what it should be

        //Update whether or not the radial menu buttons is currently pressed
        if (shouldShow(player) || isVisible()) {
            //isKeyDown breaks when using ALT for some reason, so we do it custom.
            if (getKeyBinding().isDefault()) setPressingButton(Screen.hasAltDown());
            else setPressingButton(getKeyBinding().isKeyDown());

            //Update Visibility
            if(System.currentTimeMillis()-lastSelection > 200 && isPressingButton() && shouldShow(player)) {
                raiseVisibility();
                effectHappened = false; //Make sure effect can happen again
            } else {
                decreaseVisibility();
                if(!effectHappened) { //Effect can only happen once
                    lastSelection = System.currentTimeMillis();
                    resetMenu = System.currentTimeMillis(); //Prevent instant resetting.
                    effectHappened = true;
                    triggerEffect();
                }

                //If we somehow end up in a broken decrease visibility loop
                if(System.currentTimeMillis()-resetMenu > 20000)
                    cleanup();
            }
        }
    }

    /**
     * Updates the game focus and grabs the mouse if necessary.
     */
    public void updateGameFocus() {
        //Switch the game focus state if the game is focus doesn't match the visibility.
        if ((!currentlyShown && isVisible()) || (currentlyShown && !isVisible())) {
            currentlyShown = !currentlyShown;
            if (currentlyShown) {
                getMinecraft().mouseHelper.ungrabMouse();
                if (ChiselsAndBits2.getInstance().getConfig().enableVivecraftCompatibility.get()) getMinecraft().currentScreen = this;
            } else {
                //Only grab the mouse if we're currently in our menu, so don't grab if someone is in their inventory
                if(Minecraft.getInstance().currentScreen == null || Minecraft.getInstance().currentScreen == this) {
                    getMinecraft().mouseHelper.grabMouse();
                    if (ChiselsAndBits2.getInstance().getConfig().enableVivecraftCompatibility.get())
                        getMinecraft().displayGuiScreen(null);
                }
            }
        }
    }

    //--- METHODS TO OVERWRITE ---
    /**
     * Get the key binding that needs to be pressed to turn the menu visible.
     * Needs to be set to Alt by default!
     */
    public abstract KeyBinding getKeyBinding();

    /**
     * Whether or not a player should be able to see the menu, this means whether or
     * not the player's held item has a menu.
     */
    public boolean shouldShow(final PlayerEntity player) {
        return Minecraft.getInstance().currentScreen == null || Minecraft.getInstance().currentScreen == this; //Prevent having radial menu open whilst also having another menu open.
    }

    /**
     * Triggers the effect when the menu disappears.
     */
    public abstract void triggerEffect();

    /**
     * Returns whether or not there's currently something selected.
     */
    public boolean hasSelection() {
        return hasAction();
    }

    //--- EVENTS ---
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RadialMenuEvent {
        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void onTick(final TickEvent.ClientTickEvent e) {
            final PlayerEntity player = Minecraft.getInstance().player;
            if (player == null || RADIAL_MENU == null) return; //We're not in-game yet if this happens..
            RADIAL_MENU.tick();
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void onClickWithGuiOpen(InputEvent.RawMouseEvent e) {
            if(e.getButton() == GLFW.GLFW_MOUSE_BUTTON_1 && RADIAL_MENU != null && RADIAL_MENU.isVisible()) {
                RADIAL_MENU.selectHoverOver();
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void drawLast(final RenderGameOverlayEvent.Post e) {
            if (e.getType() == RenderGameOverlayEvent.ElementType.ALL && RADIAL_MENU != null) {
                Minecraft.getInstance().getProfiler().startSection("chiselsandbits2-radialmenu");
                if (RADIAL_MENU.isVisible()) { //Render if it's visible.
                    final MainWindow window = e.getWindow();
                    RADIAL_MENU.configure(window); //Setup the height/width scales
                    if (RADIAL_MENU.isVisible()) {
                        int i = (int) (RADIAL_MENU.getMinecraft().mouseHelper.getMouseX() * (double) window.getScaledWidth() / (double) window.getWidth());
                        int j = (int) (RADIAL_MENU.getMinecraft().mouseHelper.getMouseY() * (double) window.getScaledHeight() / (double) window.getHeight());

                        //This comment makes note that the code below is horrible from a forge perspective, but it's great.
                        ForgeHooksClient.drawScreen(RADIAL_MENU, i, j, e.getPartialTicks());
                    }
                }
                Minecraft.getInstance().getProfiler().endSection();
            }
        }
    }
}
