package nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Region;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockBaked;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockSmartModel;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.RenderingManager;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.*;

public class ChiseledBlockTER {//} extends TileEntityRenderer<ChiseledBlockTileEntity> {
    /*private int isConfigured = 0;

    @Override
    public void render(ChiseledBlockTileEntity te, float v, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int partialTicks, int destroyStage) {
        if (destroyStage >= 0) {
            renderBreakingEffects(te, x, y, z, partialTicks, destroyStage);
            return;
        }
        final RenderingManager m = ChiselsAndBits2.getInstance().getClient().getRenderingManager();
        final TileChunk rc = te.getChunk(te.getWorld());
        final BlockPos chunkOffset = te.getChunk(te.getWorld()).chunkOffset();

        if(!rc.needsRebuilding() && te.getRenderTracker().isInvalid(te.getWorld(), te.getPos()))
            rc.rebuild();

        //Rebuild if necessary
        if (rc.needsRebuilding() && m.getTracker().acceptsNewTasks(rc)) {
            try {
                final Region cache = new Region(getWorld(), chunkOffset, chunkOffset.add(TileChunk.TILE_CHUNK_SIZE, TileChunk.TILE_CHUNK_SIZE, TileChunk.TILE_CHUNK_SIZE));
                final FutureTask<Tessellator> newFuture = new FutureTask<>(new BackgroundRenderer(cache, chunkOffset, te.getChunk(te.getWorld()).getTileList()));
                rc.setRenderingTask(newFuture);

                m.submit(newFuture);
                m.addFutureTracker(rc);
            } catch (RejectedExecutionException err) {
                err.printStackTrace();
            }
        }

        final GfxRenderState dl = rc.getRenderState();
        if (dl != null && dl.shouldRender()) {
            if (!dl.validForUse()) {
                rc.setRenderState(null);
                return;
            }

            GL11.glPushMatrix();
            GL11.glTranslated(-TileEntityRendererDispatcher.staticPlayerX + chunkOffset.getX(),
                    -TileEntityRendererDispatcher.staticPlayerY + chunkOffset.getY(),
                    -TileEntityRendererDispatcher.staticPlayerZ + chunkOffset.getZ());

            configureGLState();
            dl.render();
            unconfigureGLState();
            GL11.glPopMatrix();
        }
    }

    private void configureGLState() {
        isConfigured++;
        if (isConfigured == 1) {
            GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 0, 0); //lightmapTexUnit

            GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);

            GlStateManager.enableBlend();
            GlStateManager.disableAlphaTest();

            GlStateManager.enableCull();
            GlStateManager.enableTexture();

            if (Minecraft.isAmbientOcclusionEnabled())
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
            else
                GlStateManager.shadeModel(GL11.GL_FLAT);
        }
    }

    private void unconfigureGLState() {
        isConfigured--;
        if (isConfigured > 0) return;

        GlStateManager.clearCurrentColor();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
    }

    private void renderBreakingEffects(final ChiseledBlockTileEntity te, final double x, final double y, final double z, final float partialTicks, final int destroyStage) {
        bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        final String file = DESTROY_STAGES[destroyStage].toString().replace("textures/", "").replace(".png", "");
        final TextureAtlasSprite damageTexture = Minecraft.getInstance().getTextureMap().getAtlasSprite(file);
        final BlockPos cp = te.getPos();

        GlStateManager.pushMatrix();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.translated(x - cp.getX(), y - cp.getY(), z - cp.getZ());

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(0, 0, 0);

        final BlockRendererDispatcher blockRenderer = Minecraft.getInstance().getBlockRendererDispatcher();
        final BlockState estate = te.getBlockState();

        final ChiseledBlockBaked model = ChiselsAndBits2.getInstance().getClient().getRenderingManager().getCachedModel(te);

        if (!model.isEmpty()) {
            final IBakedModel damageModel = new SimpleBakedModel.Builder(estate, model, damageTexture, new Random(), System.currentTimeMillis())
                    .setTexture(damageTexture) //We set this just to avoid the RuntimeException, we don't use it.
                    .build();
            blockRenderer.getBlockModelRenderer().renderModel(te.getWorld(), damageModel, estate, cp, buffer, true, new Random(), System.currentTimeMillis());
        }

        tessellator.draw();
        buffer.setTranslation(0.0D, 0.0D, 0.0D);

        GlStateManager.clearCurrentColor();
        GlStateManager.popMatrix();
    }*/
}
