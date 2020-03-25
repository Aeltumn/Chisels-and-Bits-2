package nl.dgoossens.chiselsandbits2.client.render;

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

    private static final LineState THICK_LINE = new LineState(OptionalDouble.of(2.0));
    private static final LineState ULTRA_THICK_LINE = new LineState(OptionalDouble.of(3.0));

    public static final RenderType LINE_DEPTH = makeType(ChiselsAndBits2.MOD_ID+":lines", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
            RenderType.State.getBuilder()
                    .line(THICK_LINE)
                    .layer(PROJECTION_LAYERING)
                    .texture(NO_TEXTURE)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .depthTest(DEPTH_ALWAYS)
                    .writeMask(COLOR_WRITE)
                    .cull(CULL_DISABLED)
                    .build(false));

    public static final RenderType TAPE_MEASURE_DISTANCE = makeType(ChiselsAndBits2.MOD_ID+":tape_measure_distance", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
            RenderType.State.getBuilder()
                    .line(ULTRA_THICK_LINE)
                    .layer(PROJECTION_LAYERING)
                    .texture(NO_TEXTURE)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .depthTest(DEPTH_ALWAYS)
                    .writeMask(COLOR_WRITE)
                    .cull(CULL_DISABLED)
                    .build(false));
}
