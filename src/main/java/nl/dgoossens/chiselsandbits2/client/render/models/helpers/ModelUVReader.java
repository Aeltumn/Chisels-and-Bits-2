package nl.dgoossens.chiselsandbits2.client.render.models.helpers;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import nl.dgoossens.chiselsandbits2.common.utils.ModelUtil;

public class ModelUVReader extends ModelReader {

    public final float[] quadUVs = new float[]{0, 0, 0, 1, 1, 0, 1, 1};
    final float minU;
    final float maxUMinusMin;
    final float minV;
    final float maxVMinusMin;
    public int corners;
    int uCoord, vCoord;


    public ModelUVReader(
            final TextureAtlasSprite texture,
            final int uFaceCoord,
            final int vFaceCoord) {
        minU = texture.getMinU();
        maxUMinusMin = texture.getMaxU() - minU;

        minV = texture.getMinV();
        maxVMinusMin = texture.getMaxV() - minV;

        uCoord = uFaceCoord;
        vCoord = vFaceCoord;
    }

    @Override
    public void put(
            final int element,
            final float... data) {
        super.put(element, data);

        if (element == getVertexFormat().getElementCount() - 1) {
            if (ModelUtil.isZero(pos[uCoord]) && ModelUtil.isZero(pos[vCoord])) {
                corners = corners | 0x1;
                quadUVs[0] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[1] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isZero(pos[uCoord]) && ModelUtil.isOne(pos[vCoord])) {
                corners = corners | 0x2;
                quadUVs[4] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[5] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isOne(pos[uCoord]) && ModelUtil.isZero(pos[vCoord])) {
                corners = corners | 0x4;
                quadUVs[2] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[3] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isOne(pos[uCoord]) && ModelUtil.isOne(pos[vCoord])) {
                corners = corners | 0x8;
                quadUVs[6] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[7] = (uv[1] - minV) / maxVMinusMin;
            }
        }
    }
}