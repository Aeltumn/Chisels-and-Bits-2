package nl.dgoossens.chiselsandbits2.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IItemMenu;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.item.IMenuAction;
import nl.dgoossens.chiselsandbits2.api.radial.RadialMenu;
import nl.dgoossens.chiselsandbits2.client.ClientSide;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.impl.MenuAction;
import nl.dgoossens.chiselsandbits2.common.impl.SelectedItemMode;
import nl.dgoossens.chiselsandbits2.common.utils.ItemModeUtil;
import nl.dgoossens.chiselsandbits2.common.items.StorageItem;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ItemModeMenu extends RadialMenu {
    public final static double RING_INNER_EDGE = 20;
    public final static double RING_OUTER_EDGE = 55;
    public final static double TEXT_DISTANCE = 65;
    public final static double HALF_PI = Math.PI * 0.5;

    private IItemMode switchTo = null;
    private IMenuAction doAction = null;
    private List<MenuRegion> modes;
    private List<MenuButton> buttons;
    private long buttonLastHighlighted = 0L;
    private CustomItemRenderer cache;

    public ItemModeMenu() {
        super(new StringTextComponent("Radial Menu"));
    }

    @Override
    public KeyBinding getKeyBinding() {
        return ChiselsAndBits2.getInstance().getKeybindings().modeMenu;
    }

    @Override
    public boolean shouldShow(final PlayerEntity player) {
        return player.getHeldItemMainhand().getItem() instanceof IItemMenu;
    }

    @Override
    public boolean hasSelection() {
        return hasSwitchTo() || hasAction();
    }

    @Override
    public void triggerEffect() {
        if (hasSwitchTo() || hasAction()) {
            final float volume = ChiselsAndBits2.getInstance().getConfig().radialMenuVolume.get().floatValue();
            if (volume >= 0.0001f)
                Minecraft.getInstance().getSoundHandler().play(new SimpleSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, volume, 1.0f, Minecraft.getInstance().player.getPosition()));
        }

        if (hasSwitchTo())
            ItemModeUtil.changeItemMode(Minecraft.getInstance().player, Minecraft.getInstance().player.getHeldItemMainhand(), getSwitchTo());

        if (hasAction())
            getAction().trigger();

        //Reset actions
        switchTo = null;
        doAction = null;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!(getMinecraft().player.getHeldItemMainhand().getItem() instanceof IItemMenu)) return;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(0.0F, 0.0F, 200.0F);

        final int start = (int) (getVisibility() * 98) << 24;
        final int end = (int) (getVisibility() * 128) << 24;
        fillGradient(0, 0, getWindow().getWidth(), getWindow().getHeight(), start, end);

        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        final double middle_x = ((double) getWindow().getScaledWidth()) / 2;
        final double middle_y = ((double) getWindow().getScaledHeight()) / 2;

        switchTo = null;
        doAction = null;
        modes = getShownModes();
        buttons = getShownButtons();

        //Set the invisibility for all rendered bars.
        getItemRenderer().setAlpha(getVisibility() * 0.65f);

        renderBackgrounds(mouseX, mouseY, middle_x, middle_y, buffer);
        //Render flat coloured parts of icons and overlays.
        renderIcons(middle_x, middle_y, buffer, false);
        renderOverlay(middle_x, middle_y, buffer);
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.translatef(0.0F, 0.0F, 5.0F);
        GlStateManager.color4f(1, 1, 1, 1.0f);
        GlStateManager.enableTexture();
        GlStateManager.bindTexture(Minecraft.getInstance().getTextureMap().getGlTextureId());

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        renderIcons(middle_x, middle_y, buffer, true);
        tessellator.draw();

        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();

        //Render the overlays, there's no buffer here. This renders the texts and itemstacks.
        renderOverlay(middle_x, middle_y, null);

        if(getMinecraft().player.getHeldItemMainhand().getItem() instanceof StorageItem)
            renderCapacityBars(middle_x, middle_y);

        GlStateManager.popMatrix();
    }

    /**
     * Renders the semi-transparent backgrounds behind the buttons and regions.
     */
    private void renderBackgrounds(double mouseX, double mouseY, double middle_x, double middle_y, BufferBuilder buffer) {
        final double vecX = mouseX - middle_x;
        final double vecY = mouseY - middle_y;

        double radians = Math.atan2(vecY, vecX);
        if (radians < -HALF_PI)
            radians = radians + Math.PI * 2;

        if (!modes.isEmpty()) {
            final int totalModes = Math.max(3, modes.size());
            int currentMode = 0;
            final double perObject = 2.0 * Math.PI / totalModes;

            for (final MenuRegion mnuRgn : modes) {
                final double begin_rad = currentMode * perObject - HALF_PI;
                final double end_rad = (currentMode + 1) * perObject - HALF_PI;

                mnuRgn.x1 = Math.cos(begin_rad);
                mnuRgn.x2 = Math.cos(end_rad);
                mnuRgn.y1 = Math.sin(begin_rad);
                mnuRgn.y2 = Math.sin(end_rad);


                double fragment = Math.PI * 0.005;
                double fragment2 = Math.PI * 0.0025;

                final float a = 0.5f * getVisibility();
                float f = 0f;

                //Set the colour.
                switch(mnuRgn.type) {
                    case SELECTED:
                        f = 1;
                        fragment = Math.PI * 0.002;
                        fragment2 = Math.PI * 0.0005;
                        break;
                    case HIGHLIGHTED:
                        f = 0.8f;
                        break;
                }

                double x1m1 = Math.cos(begin_rad + fragment) * RING_INNER_EDGE;
                double x2m1 = Math.cos(end_rad - fragment) * RING_INNER_EDGE;
                double y1m1 = Math.sin(begin_rad + fragment) * RING_INNER_EDGE;
                double y2m1 = Math.sin(end_rad - fragment) * RING_INNER_EDGE;

                double x1m2 = Math.cos(begin_rad + fragment2) * RING_OUTER_EDGE;
                double x2m2 = Math.cos(end_rad - fragment2) * RING_OUTER_EDGE;
                double y1m2 = Math.sin(begin_rad + fragment2) * RING_OUTER_EDGE;
                double y2m2 = Math.sin(end_rad - fragment2) * RING_OUTER_EDGE;

                final boolean quad = inTriangle(x1m1, y1m1, x2m2, y2m2, x2m1, y2m1, vecX, vecY) || inTriangle(x1m1, y1m1, x1m2, y1m2, x2m2, y2m2, vecX, vecY);
                if (begin_rad <= radians && radians <= end_rad && quad) {
                    if(f < 0.8f) f = 0.8f;
                    mnuRgn.highlighted = true;
                    switchTo = mnuRgn.mode;
                }

                if(mnuRgn.mode instanceof SelectedItemMode && ((SelectedItemMode) mnuRgn.mode).getVoxelType() == VoxelType.COLOURED) {
                    if(f < 0.1f) f = 0.4f;
                    Color color = ((SelectedItemMode) mnuRgn.mode).getColour();
                    int red = (int) Math.round(color.getRed()*f), green = (int) Math.round(color.getGreen()*f), blue = (int) Math.round(color.getBlue()*f), alp = (int) Math.round(color.getAlpha()*a);

                    buffer.pos(middle_x + x1m1, middle_y + y1m1, blitOffset).color(red, green, blue ,alp).endVertex();
                    buffer.pos(middle_x + x2m1, middle_y + y2m1, blitOffset).color(red, green, blue ,alp).endVertex();
                    buffer.pos(middle_x + x2m2, middle_y + y2m2, blitOffset).color(red, green, blue ,alp).endVertex();
                    buffer.pos(middle_x + x1m2, middle_y + y1m2, blitOffset).color(red, green, blue ,alp).endVertex();
                } else {
                    buffer.pos(middle_x + x1m1, middle_y + y1m1, blitOffset).color(f, f, f, a).endVertex();
                    buffer.pos(middle_x + x2m1, middle_y + y2m1, blitOffset).color(f, f, f, a).endVertex();
                    buffer.pos(middle_x + x2m2, middle_y + y2m2, blitOffset).color(f, f, f, a).endVertex();
                    buffer.pos(middle_x + x1m2, middle_y + y1m2, blitOffset).color(f, f, f, a).endVertex();
                }

                currentMode++;
            }
        }

        for (final MenuButton btn : buttons) {
            final float a = 0.5f * getVisibility();
            float f = 0f;

            if (btn.x1 <= vecX && btn.x2 >= vecX && btn.y1 <= vecY && btn.y2 >= vecY) {
                f = 1;
                btn.highlighted = true;
                doAction = btn.action;
            }

            buffer.pos(middle_x + btn.x1, middle_y + btn.y1, blitOffset).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + btn.x1, middle_y + btn.y2, blitOffset).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + btn.x2, middle_y + btn.y2, blitOffset).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + btn.x2, middle_y + btn.y1, blitOffset).color(f, f, f, a).endVertex();
        }
    }

    /**
     * Renders the icons as retrieved from {@link ClientSide#getMenuActionIconLocation(IMenuAction)} or {@link ClientSide#getModeIconLocation(IItemMode)}.
     * Called both before and after textures are enabled for rendering the flat colours.
     */
    private void renderIcons(double middle_x, double middle_y, BufferBuilder buffer, boolean textures) {
        for (final MenuRegion mnuRgn : modes) {
            if(!textures) //Menu regions don't render without textures.
                continue;
            if (ClientSide.getModeIconLocation(mnuRgn.mode) == null)
                continue;

            final double x = (mnuRgn.x1 + mnuRgn.x2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
            final double y = (mnuRgn.y1 + mnuRgn.y2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
            final double x1 = x - 7;
            final double x2 = x + 9;
            final double y1 = y - 8;
            final double y2 = y + 8;

            final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureMap().getSprite(ClientSide.getModeIconLocation(mnuRgn.mode));

            final float f = 1.0f;
            final float a = 1.0f * getVisibility();

            final double u1 = 0;
            final double u2 = 16;
            final double v1 = 0;
            final double v2 = 16;

            buffer.pos(middle_x + x1, middle_y + y1, blitOffset).tex(sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1)).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + x1, middle_y + y2, blitOffset).tex(sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v2)).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + x2, middle_y + y2, blitOffset).tex(sprite.getInterpolatedU(u2), sprite.getInterpolatedV(v2)).color(f, f, f, a).endVertex();
            buffer.pos(middle_x + x2, middle_y + y1, blitOffset).tex(sprite.getInterpolatedU(u2), sprite.getInterpolatedV(v1)).color(f, f, f, a).endVertex();
        }

        for (final MenuButton btn : buttons) {
            //Depending on whether we want textures or not, continue.
            if(btn.sprite == null && textures) continue;
            if(btn.sprite != null && !textures) continue;

            final float a = 0.8f * getVisibility();

            final double u1 = 0;
            final double u2 = 16;
            final double v1 = 0;
            final double v2 = 16;

            final double btnx1 = btn.x1 + 1;
            final double btnx2 = btn.x2 - 1;
            final double btny1 = btn.y1 + 1;
            final double btny2 = btn.y2 - 1;

            final float red = ((btn.color >> 16 & 0xff) / 255.0f);
            final float green = ((btn.color >> 8 & 0xff) / 255.0f);
            final float blue = ((btn.color & 0xff) / 255.0f);

            if (btn.sprite != null) {
                TextureAtlasSprite sprite = btn.sprite;
                buffer.pos(middle_x + btnx1, middle_y + btny1, blitOffset).tex(sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1)).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx1, middle_y + btny2, blitOffset).tex(sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v2)).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx2, middle_y + btny2, blitOffset).tex(sprite.getInterpolatedU(u2), sprite.getInterpolatedV(v2)).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx2, middle_y + btny1, blitOffset).tex(sprite.getInterpolatedU(u2), sprite.getInterpolatedV(v1)).color(red, green, blue, a).endVertex();
            } else {
                if(btn.color == 0) //0 equals no colour.
                    continue;

                buffer.pos(middle_x + btnx1, middle_y + btny1, blitOffset).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx1, middle_y + btny2, blitOffset).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx2, middle_y + btny2, blitOffset).color(red, green, blue, a).endVertex();
                buffer.pos(middle_x + btnx2, middle_y + btny1, blitOffset).color(red, green, blue, a).endVertex();
            }
        }
    }

    /**
     * Render the texts next to highlighted elements and the item renders for selected item modes.
     */
    private void renderOverlay(double middle_x, double middle_y, BufferBuilder buffer) {
        boolean buttonHighlighted = false;
        for (final MenuButton btn : buttons) {
            if(buffer != null)
                continue;

            if (btn.highlighted) {
                buttonHighlighted = true;
                final String text = btn.name;
                int c = new Color(1.0f, 1.0f, 1.0f, getVisibility()).hashCode();
                if (btn.textSide == Direction.WEST) {
                    getFontRenderer().drawStringWithShadow(text, (int) (middle_x + btn.x1 - 8) - getFontRenderer().getStringWidth(text), (int) (middle_y + btn.y1 + 6), c);
                } else if (btn.textSide == Direction.EAST) {
                    getFontRenderer().drawStringWithShadow(text, (int) (middle_x + btn.x2 + 8), (int) (middle_y + btn.y1 + 6), c);
                } else if (btn.textSide == Direction.UP) {
                    getFontRenderer().drawStringWithShadow(text, (int) (middle_x + (btn.x1 + btn.x2) * 0.5 - getFontRenderer().getStringWidth(text) * 0.5), (int) (middle_y + btn.y1 - 14), c);
                } else if (btn.textSide == Direction.DOWN) {
                    getFontRenderer().drawStringWithShadow(text, (int) (middle_x + (btn.x1 + btn.x2) * 0.5 - getFontRenderer().getStringWidth(text) * 0.5), (int) (middle_y + btn.y1 + 24), c);
                }
            }
        }
        if(buttonHighlighted) buttonLastHighlighted = System.currentTimeMillis();
        //Nicely fade it back in when you release a button.
        int remove = (int) Math.round((212.0d-64.0d) * (1.0-(Math.min((double)(System.currentTimeMillis()-buttonLastHighlighted), 3000.0d) / 3000.0d)));

        final double w = 8;
        RenderHelper.enableGUIStandardItemLighting();
        for (final MenuRegion mnuRgn : modes) {
            if (buffer == null && (mnuRgn.highlighted || mnuRgn.type.isSelected())) {
                final double x = (mnuRgn.x1 + mnuRgn.x2) * 0.5;
                final double y = (mnuRgn.y1 + mnuRgn.y2) * 0.5;
                Color base = new Color(255, 255, 255);
                if(mnuRgn.mode instanceof SelectedItemMode && ((SelectedItemMode) mnuRgn.mode).getVoxelType() == VoxelType.COLOURED)
                    base = ((SelectedItemMode) mnuRgn.mode).getColour();
                int color = new Color(base.getRed(), base.getGreen(), base.getBlue(),(int) Math.round((212-(!mnuRgn.highlighted ? remove : 0)) * getVisibility() * (((double)base.getAlpha())/255))).hashCode();

                int fixed_x = (int) (x * TEXT_DISTANCE);
                final int fixed_y = (int) (y * TEXT_DISTANCE);
                final String text = mnuRgn.mode.getLocalizedName();

                if (x <= -0.2) {
                    fixed_x -= getFontRenderer().getStringWidth(text);
                } else if (-0.2 <= x && x <= 0.2) {
                    fixed_x -= getFontRenderer().getStringWidth(text) / 2;
                }

                GlStateManager.enableBlend();
                GlStateManager.disableAlphaTest();
                getFontRenderer().drawStringWithShadow(text, (int) middle_x + fixed_x, (int) middle_y + fixed_y, color);
            }
            if (mnuRgn.mode instanceof SelectedItemMode) {
                if (SelectedItemMode.isNone(mnuRgn.mode)) continue;

                //Selectable blocks should render the item that's inside!
                final double x = (mnuRgn.x1 + mnuRgn.x2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
                final double y = (mnuRgn.y1 + mnuRgn.y2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
                if(((SelectedItemMode) mnuRgn.mode).getVoxelType() == VoxelType.COLOURED) {
                    //We're currently not using this system.
                    /*if(buffer==null)
                        continue;

                    Color color = ((SelectedItemMode) mnuRgn.mode).getColour();
                    int red = color.getRed(), green = color.getGreen(), blue = color.getBlue(), a = (int) Math.round(color.getAlpha()*0.6f);
                    final double btnx1 = x-8 + 1;
                    final double btnx2 = x-8 + 16 - 1;
                    final double btny1 = y-8 + 1;
                    final double btny2 = y-8 + 16 - 1;

                    buffer.pos(middle_x + btnx1, middle_y + btny1, blitOffset).color(red, green, blue, a).endVertex();
                    buffer.pos(middle_x + btnx1, middle_y + btny2, blitOffset).color(red, green, blue, a).endVertex();
                    buffer.pos(middle_x + btnx2, middle_y + btny2, blitOffset).color(red, green, blue, a).endVertex();
                    buffer.pos(middle_x + btnx2, middle_y + btny1, blitOffset).color(red, green, blue, a).endVertex();*/
                } else {
                    if(buffer!=null)
                        continue;

                    //We use minecrafts (actually forges) item renderer instead of our own here because we can't support alpha in item rendering anyways.
                    getMinecraft().getItemRenderer().renderItemAndEffectIntoGUI(null, ((SelectedItemMode) mnuRgn.mode).getStack(), (int) Math.round(middle_x + x - w), (int) Math.round(middle_y + y - w));
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    /**
     * Render durability bars showing how full slots are.
     */
    private void renderCapacityBars(double middle_x, double middle_y) {
        ItemStack stack = getMinecraft().player.getHeldItemMainhand();
        BitStorage store = stack.getCapability(StorageCapabilityProvider.STORAGE).orElse(null);
        if(store == null) return; //If it's null we can't do nothing.

        for (final MenuRegion mnuRgn : modes) {
            if (mnuRgn.mode instanceof SelectedItemMode) {
                SelectedItemMode s = (SelectedItemMode) mnuRgn.mode;
                //None or bookmarks don't have amounts.
                if (SelectedItemMode.isNone(s) || s.getVoxelType() == VoxelType.COLOURED) continue;

                //Selectable blocks should render the item that's inside!
                final double x = (mnuRgn.x1 + mnuRgn.x2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
                final double y = (mnuRgn.y1 + mnuRgn.y2) * 0.5 * (RING_OUTER_EDGE * 0.6 + 0.4 * RING_INNER_EDGE);
                getItemRenderer().renderDurabilityBar(ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get() - store.get(s.getVoxelWrapper()),
                        ChiselsAndBits2.getInstance().getConfig().bitsPerTypeSlot.get(), (int) Math.round(middle_x + x - 9), (int) Math.round(middle_y + y - 7));
            }
        }
    }

    public List<MenuRegion> getShownModes() {
        List<MenuRegion> modes = new ArrayList<>();

        //Setup mode regions
        for (IItemMode m : ItemModeUtil.getItemMode(getMinecraft().player.getHeldItemMainhand()).getType().getItemModes(getMinecraft().player.getHeldItemMainhand()))
            modes.add(new MenuRegion(m, getMinecraft().player.getHeldItemMainhand(), getMinecraft().player));

        return modes;
    }

    public List<MenuButton> getShownButtons() {
        List<MenuButton> buttons = new ArrayList<>();

        buttons.add(new MenuButton(MenuAction.UNDO, TEXT_DISTANCE, -20, Direction.EAST));
        buttons.add(new MenuButton(MenuAction.REDO, TEXT_DISTANCE, 4, Direction.EAST));

        if(getMinecraft().player.getHeldItemMainhand().getItem() instanceof IItemMenu) {
            Set<MenuButton> i = ((IItemMenu) getMinecraft().player.getHeldItemMainhand().getItem()).getMenuButtons(getMinecraft().player.getHeldItemMainhand());
            if(i != null) buttons.addAll(i);
        }
        return buttons;
    }

    public CustomItemRenderer getItemRenderer() {
        if(cache == null) cache = new CustomItemRenderer(Minecraft.getInstance().getItemRenderer());
        return cache;
    }

    public boolean hasSwitchTo() {
        return switchTo != null;
    }

    public IItemMode getSwitchTo() {
        return switchTo;
    }

    public boolean hasAction() {
        return doAction != null;
    }

    public IMenuAction getAction() {
        return doAction;
    }

    public static class MenuButton {
        final MenuAction action;
        TextureAtlasSprite sprite;
        double x1, x2;
        double y1, y2;
        boolean highlighted;
        int color;
        String name;
        Direction textSide;

        public MenuButton(final MenuAction action, final double x, final double y, final Direction textSide) {
            this(action, x, y, 0xffffffff, textSide);
            sprite = Minecraft.getInstance().getTextureMap().getSprite(ClientSide.getMenuActionIconLocation(action));
        }

        public MenuButton(final MenuAction action, final double x, final double y, final int col, final Direction textSide) {
            if(action == null) throw new RuntimeException("Menu buttons need an action!");

            this.name = action.getLocalizedName();
            this.action = action;
            x1 = x;
            x2 = x + 18;
            y1 = y;
            y2 = y + 18;
            color = col;
            this.textSide = textSide;
        }

        public MenuAction getMenuAction() {
            return action;
        }
    }

    public static enum RegionType {
        SELECTED, //For the currently selected mode.
        HIGHLIGHTED, //For selected but not used selected item mode
        DEFAULT,
        ;

        public boolean isSelected() {
            return this == SELECTED || this == HIGHLIGHTED;
        }
    }

    public static class MenuRegion {
        public final IItemMode mode;
        public RegionType type;
        public double x1, x2;
        public double y1, y2;
        public boolean highlighted;

        public MenuRegion(final IItemMode mode, final ItemStack stack, final PlayerEntity player) {
            this.mode = mode;
            SelectedItemMode s = ItemModeUtil.getSelectedItemMode(stack);
            if (s == null) type = ItemModeUtil.getItemMode(stack).equals(mode) ? RegionType.SELECTED : RegionType.DEFAULT;
            else {
                if(!s.equals(mode) || SelectedItemMode.isNone(mode)) {
                    type = RegionType.DEFAULT;
                    return;
                }
                int b = ItemModeUtil.getGlobalSelectedBit(player);
                if(b != s.getBitId()) {
                    //Highlighted = selected in this bitstorage but won't be used atm to build.
                    type = RegionType.HIGHLIGHTED;
                } else
                    //Selected in this storage and going to get used.
                    type = RegionType.SELECTED;
            }
        }
    }
}
