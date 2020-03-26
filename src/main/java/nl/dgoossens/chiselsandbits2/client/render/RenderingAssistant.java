package nl.dgoossens.chiselsandbits2.client.render;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Assists with generic rendering tasks for rendering boxes and lines, mostly those called by the ClientSideHelper.
 */
public class RenderingAssistant {
    public static void drawBoundingBox(final MatrixStack matrix, final IRenderTypeBuffer builder, final AxisAlignedBB bb, final BlockPos location) {
        drawBoundingBox(matrix, builder, bb, location, 0.0f, 0.0f, 0.0f);
    }

    public static void drawBoundingBox(final MatrixStack matrix, final IRenderTypeBuffer builder, final AxisAlignedBB bb, final BlockPos location, final float red, final float green, final float blue) {
        if (bb != null) {
            Preconditions.checkState(matrix.clear(), "Matrix stack should be cleared!");
            ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            double x = renderInfo.getProjectedView().x, y = renderInfo.getProjectedView().y, z = renderInfo.getProjectedView().z;

            //Draw one with the normal non-depth lines version and one much lighter with depth test
            //This makes it much easier to sculpt as you can estimate how many bits you will destroy with this action.
            matrix.push();
            matrix.translate(location.getX() - x, location.getY() - y, location.getZ() - z);
            WorldRenderer.drawBoundingBox(matrix, builder.getBuffer(ChiselsAndBitsRenderTypes.LINES), bb, red, green, blue, 0.28f);
            WorldRenderer.drawBoundingBox(matrix, builder.getBuffer(ChiselsAndBitsRenderTypes.LINES_BACK_EDGES), bb, red, green, blue, 0.12f);
            matrix.pop();

        }
    }

    public static void drawLine(final MatrixStack matrix, final IRenderTypeBuffer buffer, final Vec3d a, final Vec3d b, final float red, final float green, final float blue) {
        if(a != null && b != null) {
            Preconditions.checkState(matrix.clear(), "Matrix stack should be cleared!");

            ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            double x = renderInfo.getProjectedView().x, y = renderInfo.getProjectedView().y, z = renderInfo.getProjectedView().z;
            matrix.push();
            matrix.translate(-x, -y, -z);
            renderLine(matrix, buffer, a, b, red, green, blue);
            matrix.pop();
        }
    }

    private static void renderLine(final MatrixStack matrix, final IRenderTypeBuffer buffer, final Vec3d a, final Vec3d b, final float red, final float green, final float blue) {
        IVertexBuilder builder = buffer.getBuffer(ChiselsAndBitsRenderTypes.TAPE_MEASURE_DISTANCE);
        Matrix4f matrix4f = matrix.getLast().getMatrix();
        builder.pos(matrix4f, (float) a.getX(), (float) a.getY(), (float) a.getZ()).color(red, green, blue, 0.4f).endVertex();
        builder.pos(matrix4f, (float) b.getX(), (float) b.getY(), (float) b.getZ()).color(red, green, blue, 0.4f).endVertex();
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
