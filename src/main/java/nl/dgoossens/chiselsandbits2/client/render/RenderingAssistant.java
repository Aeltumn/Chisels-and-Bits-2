package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledBlockColor;
import nl.dgoossens.chiselsandbits2.client.render.color.ChiseledTintColor;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Assists with generic rendering tasks, mostly those called by the ClientSideHelper.
 */
public class RenderingAssistant {
    public static void drawSelectionBoundingBoxIfExists(final AxisAlignedBB bb, final BlockPos blockPos, final PlayerEntity player, final float partialTicks, final boolean normalBoundingBox, final int red, final int green, final int blue, final int alpha, final int seeThruAlpha) {
        if (bb != null) {
            final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            final double x = renderInfo.getProjectedView().x;
            final double y = renderInfo.getProjectedView().y;
            final double z = renderInfo.getProjectedView().z;

            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glLineWidth(2.0F);
            GlStateManager.disableTexture();
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_FLAT);

            final AxisAlignedBB bb2 = bb.expand(0.002D, 0.002D, 0.002D).offset(-x + blockPos.getX(), -y + blockPos.getY(), -z + blockPos.getZ());
            if (!normalBoundingBox)
                renderBoundingBox(bb2, red, green, blue, alpha);

            GlStateManager.disableDepthTest();
            renderBoundingBox(bb2, red, green, blue, seeThruAlpha);

            GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture();
            GlStateManager.disableBlend();
        }
    }

    public static void drawLineWithColor(final Vec3d a, final Vec3d b, final BlockPos blockPos, final PlayerEntity player, final float partialTicks, final boolean normalBoundingBox, final int red, final int green, final int blue, final int alpha, final int seeThruAlpha) {
        if(a != null && b != null) {
            final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            final double x = renderInfo.getProjectedView().x;
            final double y = renderInfo.getProjectedView().y;
            final double z = renderInfo.getProjectedView().z;

            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glLineWidth(2.0F);
            GlStateManager.disableTexture();
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_FLAT);

            final Vec3d a2 = a.add(-x + blockPos.getX(), -y + blockPos.getY(), -z + blockPos.getZ());
            final Vec3d b2 = b.add(-x + blockPos.getX(), -y + blockPos.getY(), -z + blockPos.getZ());
            if(!normalBoundingBox)
                renderLine(a2, b2, red, green, blue, alpha);

            GlStateManager.disableDepthTest();
            renderLine(a2, b2, red, green, blue, seeThruAlpha);

            GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture();
            GlStateManager.disableBlend();
        }
    }

    // Custom replacement of 1.9.4 -> 1.10's method that changed.
    public static void renderBoundingBox(final AxisAlignedBB boundingBox, final int red, final int green, final int blue, final int alpha) {
        final Tessellator tess = Tessellator.getInstance();
        final BufferBuilder buffer = tess.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        final double minX = boundingBox.minX;
        final double minY = boundingBox.minY;
        final double minZ = boundingBox.minZ;
        final double maxX = boundingBox.maxX;
        final double maxY = boundingBox.maxY;
        final double maxZ = boundingBox.maxZ;

        // lower ring ( starts to 0 / 0 )
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();

        // Y line at 0 / 0
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

        // upper ring ( including previous point to draw 4 lines )
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

        /*
         * the next 3 Y Lines use flat shading to render invisible lines to
         * enable doing this all in one pass.
         */

        // Y line at 1 / 0
        buffer.pos(maxX, minY, minZ).color(red, green, blue, 0).endVertex();
        buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();

        // Y line at 0 / 1
        buffer.pos(minX, minY, maxZ).color(red, green, blue, 0).endVertex();
        buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

        // Y line at 1 / 1
        buffer.pos(maxX, minY, maxZ).color(red, green, blue, 0).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

        tess.draw();
    }

    public static void renderLine(final Vec3d a, final Vec3d b, final int red, final int green, final int blue, final int alpha) {
        final Tessellator tess = Tessellator.getInstance();
        final BufferBuilder buffer = tess.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(a.getX(), a.getY(), a.getZ()).color(red, green, blue, alpha).endVertex();
        buffer.pos(b.getX(), b.getY(), b.getZ()).color(red, green, blue, alpha).endVertex();

        tess.draw();
    }

    public static void renderQuads(final int alpha, final BufferBuilder renderer, final List<BakedQuad> quads, final ChiseledTintColor colorProvider, final boolean showSilhouette) {
        int i = 0;
        for (final int j = quads.size(); i < j; ++i) {
            final BakedQuad bakedquad = quads.get(i);
            Color color = showSilhouette ? new Color(45, 45, 45, alpha) : new Color(colorProvider.getColor(bakedquad.getTintIndex()));
            //net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, bakedquad, (color.hashCode() & 0x00ffffff) + ((showSilhouette ? 55 : alpha) << 24));
        }
    }

    public static void renderModel(final int alpha, final IBakedModel model, final ChiseledTintColor colorProvider, final boolean showSilhoutte) {
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        Random r = new Random();
        for (final Direction face : Direction.values()) {
            r.setSeed(42);
            renderQuads(alpha, buffer, model.getQuads(null, face, r), colorProvider, showSilhoutte);
        }

        r.setSeed(42);
        renderQuads(alpha, buffer, model.getQuads(null, null, r), colorProvider, showSilhoutte);
        tessellator.draw();
    }

    public static void renderGhostModel(final IBakedModel baked, final World worldObj, final float partialTicks, final BlockPos blockPos, final boolean notPlaceable, final boolean expand) {
        //GlStateManager.bindTexture(Minecraft.getInstance().getTextureMap().getGlTextureId());
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.enableBlend();
        GlStateManager.enableTexture();
        //GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        ChiseledTintColor colorProvider = new ChiseledBlockColor(worldObj, blockPos); //Pass the location so grass/water is tinted properly
        if(expand) {
            //Expand just a slight bit larger so no z-fighting occurs
            GlStateManager.scaled(1.02 ,1.02, 1.02);
            GlStateManager.translated(-0.005, -0.005, -0.005);
        }
        renderModel(Math.max(3, Math.round(10.0f * Math.max(worldObj.getLightFor(LightType.BLOCK, blockPos), worldObj.getLightFor(LightType.SKY, blockPos)))), baked, colorProvider, notPlaceable);
        GlStateManager.disableBlend();
    }
}
