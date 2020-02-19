package nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import org.lwjgl.opengl.GL11;

public abstract class GfxRenderState {
    //Counter which increases after each TextureStitchEvent. Byte to minimise space used. (rolling over from 127 to -128 doesn't matter because we only do equals checks)
    public static byte gfxRefresh = 0;

    public static GfxRenderState getNewState(final int vertexCount) {
        return vertexCount == 0 ? new VoidRenderState() : new VBORenderState();
    }

    public abstract boolean validForUse();

    public abstract boolean render();

    public abstract GfxRenderState prepare(final Tessellator t);

    public abstract void destroy();

    public boolean shouldRender() {
        return true;
    }

    //--- VOID ---
    public static class VoidRenderState extends GfxRenderState {
        @Override
        public boolean validForUse() {
            return true;
        }

        @Override
        public boolean render() {
            return false;
        }

        @Override
        public boolean shouldRender() {
            return false;
        }

        @Override
        public GfxRenderState prepare(final Tessellator t) {
            if (t.getBuffer().getVertexFormat().getSize() > 0)
                return GfxRenderState.getNewState(t.getBuffer().getVertexFormat().getElements().size()).prepare(t);
            t.getBuffer().finishDrawing();
            return this;
        }

        @Override
        public void destroy() {
        }
    }

    //--- VBO ---
    public static class VBORenderState extends GfxRenderState {
        byte refreshNum;
        VertexBuffer vertexbuffer;

        @Override
        public boolean validForUse() {
            return refreshNum == gfxRefresh;
        }

        @Override
        public boolean render() {
            /*if (vertexbuffer != null) {
                GlStateManager.enableClientState(32884);
                GLX.glClientActiveTexture(GLX.GL_TEXTURE0); //defaultTexUnit
                GlStateManager.enableClientState(32888);
                GLX.glClientActiveTexture(GLX.GL_TEXTURE1); //lightmapTexUnit
                GlStateManager.enableClientState(32888);
                GLX.glClientActiveTexture(GLX.GL_TEXTURE0); //defaultTexUnit
                GlStateManager.enableClientState(32886);

                vertexbuffer.bindBuffer();
                setupArrayPointers();
                vertexbuffer.drawArrays(GL11.GL_QUADS);
                GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
                GlStateManager.clearCurrentColor();

                for (final VertexFormatElement vertexformatelement : DefaultVertexFormats.BLOCK.getElements()) {
                    final VertexFormatElement.Usage vertexformatelement$enumusage = vertexformatelement.getUsage();
                    final int i = vertexformatelement.getIndex();

                    switch (vertexformatelement$enumusage) {
                        case POSITION:
                            GlStateManager.disableClientState(32884);
                            break;
                        case UV:
                            GLX.glClientActiveTexture(GLX.GL_TEXTURE0 + i); //defaultTexUnit
                            GlStateManager.disableClientState(32888);
                            GLX.glClientActiveTexture(GLX.GL_TEXTURE0); //defaultTexUnit
                            break;
                        case COLOR:
                            GlStateManager.disableClientState(32886);
                            GlStateManager.clearCurrentColor();
                        default:
                            break;
                    }
                }
                return true;
            }*/
            return false;
        }

        /*private void setupArrayPointers() {
            GlStateManager.vertexPointer(3, GL11.GL_FLOAT, 28, 0);
            GlStateManager.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
            GlStateManager.texCoordPointer(2, GL11.GL_FLOAT, 28, 16);
            GLX.glClientActiveTexture(GLX.GL_TEXTURE1); //lightmapTexUnit
            GlStateManager.texCoordPointer(2, GL11.GL_SHORT, 28, 24);
            GLX.glClientActiveTexture(GLX.GL_TEXTURE0); //defaultTexUnit
        }*/

        @Override
        public GfxRenderState prepare(final Tessellator t) {
            if (t.getBuffer().getVertexFormat().getSize() == 0) {
                destroy();
                return new VoidRenderState().prepare(t);
            }

            destroy();
            if (vertexbuffer == null) vertexbuffer = new VertexBuffer(t.getBuffer().getVertexFormat());

            t.getBuffer().finishDrawing();
            //vertexbuffer.upload(t.getBuffer().getNextBuffer());
            refreshNum = gfxRefresh;
            return this;
        }

        @Override
        public void destroy() {
            if (vertexbuffer != null) {
                //vertexbuffer.deleteGlBuffers();
                vertexbuffer = null;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            //if (vertexbuffer != null)
                //ChiselsAndBits2.getInstance().getClient().getRenderingManager().addNextFrameTask(vertexbuffer::deleteGlBuffers);
        }
    }
}
