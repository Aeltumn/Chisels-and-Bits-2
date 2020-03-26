package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;

/**
 * Renderer in charge of rendering the labels for the tape measure.
 */
public class TapeMeasureRenderer {
    /**
     * Renders the label next to the tape measure bounding box.
     */
    public static void renderTapeMeasureLabel(MatrixStack matrix, IRenderTypeBuffer buffer, final float partialTicks, final double x, final double y, final double z, final double len, final int red, final int green, final int blue) {
        final double letterSize = 5.0;
        final float zScale = 0.001f;

        final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        final String text = formatTapeMeasureLabel(len);
        matrix.push();

        //Translate matrix to adjust for the projected view and translate to sign location and scale the text up
        ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        double rx = renderInfo.getProjectedView().x, ry = renderInfo.getProjectedView().y, rz = renderInfo.getProjectedView().z;
        matrix.translate(x - rx, y + getScale(len) * letterSize - ry, z - rz);
        matrix.scale(getScale(len), -getScale(len), zScale);

        //Rotate towards player
        //TODO rotate the text towards the viewing entity
        //matrix.rotate(new Quaternion(180 - renderInfo.getYaw(), 0f, 1f, 0f));
        //matrix.rotate(new Quaternion(-renderInfo.getPitch(), 1f, 0f, 0f));


        fontRenderer.renderString(text, (float)(-fontRenderer.getStringWidth(text) / 2), 0, red << 16 | green << 8 | blue, true, matrix.getLast().getMatrix(), buffer, false, 0, 15728880);
        matrix.pop();
    }

    /**
     * Get the scale of the tape measure label based on the length of the measured area.
     */
    private static float getScale(final double maxLen) {
        final double maxFontSize = 0.04;
        final double minFontSize = 0.004;

        final double delta = Math.min(1.0, maxLen / 4.0);
        double scale = maxFontSize * delta + minFontSize * (1.0 - delta);
        if (maxLen < 0.25)
            scale = minFontSize;

        return (float) Math.min(maxFontSize, scale);
    }

    /**
     * Format the label of the tape measure into the proper format.
     */
    private static String formatTapeMeasureLabel(final double d) {
        final double blocks = Math.floor(d);
        final double bits = d - blocks;

        final StringBuilder b = new StringBuilder();

        if (blocks > 0)
            b.append((int) blocks).append("m");

        if (bits * 16 > 0.9999) {
            if (b.length() > 0)
                b.append(" ");
            b.append((int) (bits * 16)).append("b");
        }

        return b.toString();
    }
}
