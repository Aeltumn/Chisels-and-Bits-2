package nl.dgoossens.chiselsandbits2.client.render.model.helpers;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;

public abstract class BaseModelReader implements IVertexConsumer {

    @Override
    public VertexFormat getVertexFormat() {
        return DefaultVertexFormats.BLOCK;
    }

    @Override
    public void setQuadTint(
            final int tint) {
    }

    @Override
    public void setQuadOrientation(
            final Direction orientation) {
    }

    @Override
    public void setApplyDiffuseLighting(
            final boolean diffuse) {

    }

    @Override
    public void setTexture(
            final TextureAtlasSprite texture) {

    }
}
