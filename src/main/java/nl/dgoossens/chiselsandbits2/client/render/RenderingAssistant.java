package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
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
 * Assists with generic rendering tasks for rendering boxes and lines, mostly those called by the ClientSideHelper.
 */
public class RenderingAssistant {
    public static void drawBoundingBox(final MatrixStack matrix, final IRenderTypeBuffer builder, final AxisAlignedBB bb, final BlockPos location) {
        drawBoundingBox(matrix, builder, bb, location, 0.0f, 0.0f, 0.0f);
    }

    public static void drawBoundingBox(final MatrixStack matrix, final IRenderTypeBuffer builder, final AxisAlignedBB bb, final BlockPos location, final float red, final float green, final float blue) {
        if (bb != null) {
            ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            double x = renderInfo.getProjectedView().x, y = renderInfo.getProjectedView().y, z = renderInfo.getProjectedView().z;
            AxisAlignedBB offsetBoundingBox = bb.offset(location.getX() - x, location.getY() - y, location.getZ() - z);
            WorldRenderer.drawBoundingBox(matrix, builder.getBuffer(RenderType.getLines()), offsetBoundingBox, red, green, blue, 0.4f);
        }
    }

    public static void drawLine(final Vec3d a, final Vec3d b, final float red, final float green, final float blue) {
        if(a != null && b != null) {
            ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            double x = renderInfo.getProjectedView().x, y = renderInfo.getProjectedView().y, z = renderInfo.getProjectedView().z;

            //TODO Don't know if RenderSystem is the best way to do this.
            //RenderSystem.enableBlend();
            //RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            //GL11.glLineWidth(2.0F);
            //RenderSystem.disableTexture();
            //RenderSystem.depthMask(false);
            //RenderSystem.shadeModel(GL11.GL_FLAT);
            renderLine(a.subtract(x, y, z), b.subtract(x, y, z), red, green, blue, 0.4f);

            //RenderSystem.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            //RenderSystem.depthMask(true);
            //RenderSystem.enableTexture();
            //RenderSystem.disableBlend();
        }
    }

    private static void renderLine(final Vec3d a, final Vec3d b, final float red, final float green, final float blue, final float alpha) {
        MatrixStack matrix = new MatrixStack();
        matrix.push();
        IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IVertexBuilder builder = buffer.getBuffer(RenderType.getLines());
        builder.pos(a.getX(), a.getY(), a.getZ()).color(red, green, blue, alpha).endVertex();
        builder.pos(b.getX(), b.getY(), b.getZ()).color(red, green, blue, alpha).endVertex();
        matrix.pop();
        buffer.finish();
    }

    /*public static void renderQuads(final int alpha, final BufferBuilder renderer, final List<BakedQuad> quads, final ChiseledTintColor colorProvider, final boolean showSilhouette) {
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
    }*/
}
