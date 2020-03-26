package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

/**
 * The class where we declare all render types used by Chisels & Bits 2.
 */
public class ChiselsAndBitsRenderTypes extends RenderType {
    //Generic constructor to match super
    public ChiselsAndBitsRenderTypes(String a, VertexFormat b, int c, int d, boolean e, boolean f, Runnable g, Runnable h) {
        super(a, b, c, d, e, f, g, h);
    }

    private static final LineState LINE = new LineState(OptionalDouble.of(3));
    private static final LineState WIDE_LINE = new LineState(OptionalDouble.of(5));
    private static final DepthTestState DEPTH_FRONT = new DepthTestState(GL11.GL_LESS);
    private static final DepthTestState DEPTH_BACK = new DepthTestState(GL11.GL_GEQUAL);
    private static final DepthTestState DEPTH_ALWAYS_SHOW = new DepthTestState(GL11.GL_ALWAYS);

    public static final RenderType LINES = makeType(ChiselsAndBits2.MOD_ID+":lines", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
            RenderType.State.getBuilder()
                    .line(LINE)
                    .layer(PROJECTION_LAYERING)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .writeMask(COLOR_WRITE)
                    .depthTest(DEPTH_FRONT)
                    .build(false));

    public static final RenderType LINES_BACK_EDGES = makeType(ChiselsAndBits2.MOD_ID+":lines_back_edges", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
            RenderType.State.getBuilder()
                    .line(LINE)
                    .layer(PROJECTION_LAYERING)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .depthTest(DEPTH_BACK)
                    .writeMask(COLOR_WRITE)
                    .build(false));

    public static final RenderType TAPE_MEASURE_DISTANCE = makeType(ChiselsAndBits2.MOD_ID+":tape_measure_distance", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
            RenderType.State.getBuilder()
                    .line(WIDE_LINE)
                    .layer(PROJECTION_LAYERING)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .depthTest(DEPTH_ALWAYS_SHOW)
                    .writeMask(COLOR_WRITE)
                    .build(false));

    /**
     * Extended line states remain exactly the value in pixels wide on the screen
     * no matter how far away the player is.
     */
    public static class ExtendedLineState extends RenderState.LineState {
        private double value;
        public ExtendedLineState(double value) {
            super(OptionalDouble.empty());
            this.value = value;
        }

        @Override
        public void setupRenderState() {
            //We scale the line width with the screen size of the user.
            RenderSystem.lineWidth((float) value * Math.max(2.5F, (float) Minecraft.getInstance().getMainWindow().getFramebufferWidth() / 1920.0F * 2.5F));
        }

        @Override
        public void clearRenderState() {
            RenderSystem.lineWidth(1.0F);
        }
    }
}
