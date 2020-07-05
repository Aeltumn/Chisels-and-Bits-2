package nl.dgoossens.chiselsandbits2.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Custom renderer to render durability bars with transparency support.
 */
@OnlyIn(Dist.CLIENT)
public class DurabilityBarRenderer {
    private float alpha = 1.0f;

    public void setAlpha(float a) {
        alpha = a;
    }

    public void renderDurabilityBar(double damage, double maxDamage, int xPosition, int yPosition) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.disableTexture();
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        double health = damage / maxDamage;
        int i = Math.round(13.0F - (float) health * 13.0F);
        int j = MathHelper.hsvToRGB(Math.max(0.0F, (float) (1.0F - health)) / 3.0F, 1.0F, 1.0F);
        this.draw(bufferbuilder, xPosition + 2, yPosition + 13, 13, 2, 0, 0, 0);
        this.draw(bufferbuilder, xPosition + 2, yPosition + 13, i, 1, j >> 16 & 255, j >> 8 & 255, j & 255);

        GlStateManager.enableLighting();
        GlStateManager.enableTexture();
    }

    //Draw with alpha support.
    private void draw(BufferBuilder renderer, int x, int y, int width, int height, int red, int green, int blue) {
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int a = Math.round(alpha * 255);
        renderer.pos(x, y, 0.0D).color(red, green, blue, a).endVertex();
        renderer.pos(x, y + height, 0.0D).color(red, green, blue, a).endVertex();
        renderer.pos(x + width, y + height, 0.0D).color(red, green, blue, a).endVertex();
        renderer.pos(x + width, y, 0.0D).color(red, green, blue, a).endVertex();
        Tessellator.getInstance().draw();
    }
}