package nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.render.IFaceBuilder;

import java.util.concurrent.ConcurrentHashMap;

public class ChiselsAndBitsBakedQuad extends BakedQuad {
    public ChiselsAndBitsBakedQuad(final float[][][] unpackedData, int tint, Direction orientation, TextureAtlasSprite sprite) {
        super(ChiselsAndBits2.getInstance().getClient().getRenderingManager().getFormatInfo().pack(unpackedData), tint, orientation, sprite, true);
    }

    private float[] getRawPart(int v, int i) {
        return ChiselsAndBits2.getInstance().getClient().getRenderingManager().getFormatInfo().unpack(vertexData, v, i);
    }

    @Override
    public void pipe(final IVertexConsumer consumer) {
        final int[] eMap = LightUtil.mapFormats(consumer.getVertexFormat(), DefaultVertexFormats.BLOCK);

        consumer.setTexture(sprite);
        consumer.setQuadTint(getTintIndex());
        consumer.setQuadOrientation(getFace());
        consumer.setApplyDiffuseLighting(true);

        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < consumer.getVertexFormat().getElements().size(); e++)
                consumer.put(e, getRawPart(v, eMap[e]));
        }
    }

    @Override
    public int[] getVertexData() {
        final int[] tmpData = new int[DefaultVertexFormats.BLOCK.getSize() /* / 4 * 4 */];

        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < DefaultVertexFormats.BLOCK.getElements().size(); e++)
                LightUtil.pack(getRawPart(v, e), tmpData, DefaultVertexFormats.BLOCK, v, e);
        }

        return tmpData;
    }

    public static class Builder implements IVertexConsumer, IFaceBuilder {
        private final VertexFormat format;
        private float[][][] unpackedData;
        private int tint = -1;
        private Direction orientation;
        private int vertices = 0;
        private int elements = 0;

        public Builder(VertexFormat format) {
            this.format = format;
        }

        @Override
        public VertexFormat getVertexFormat() {
            return format;
        }

        @Override
        public void setQuadTint(final int tint) {
            this.tint = tint;
        }

        @Override
        public void setQuadOrientation(final Direction orientation) {
            this.orientation = orientation;
        }

        @Override
        public void put(final int element, final float... data) {
            for (int i = 0; i < 4; i++) {
                if (i < data.length)
                    unpackedData[vertices][element][i] = data[i];
                else
                    unpackedData[vertices][element][i] = 0;
            }

            elements++;

            if (elements == getVertexFormat().getElements().size()) {
                vertices++;
                elements = 0;
            }
        }

        @Override
        public void begin() {
            unpackedData = new float[4][getVertexFormat().getElements().size()][4];
            tint = -1;
            orientation = null;

            vertices = 0;
            elements = 0;
        }

        @Override
        public BakedQuad create(final TextureAtlasSprite sprite) {
            return new ChiselsAndBitsBakedQuad(unpackedData, tint, orientation, sprite);
        }

        @Override
        public void setFace(final Direction myFace, final int tintIndex) {
            setQuadOrientation(myFace);
            setQuadTint(tintIndex);
        }

        @Override
        public void setApplyDiffuseLighting(final boolean diffuse) {
        }

        @Override
        public void setTexture(final TextureAtlasSprite texture) {
        }

        @Override
        public VertexFormat getFormat() {
            return format;
        }
    }
}
