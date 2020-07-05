package nl.dgoossens.chiselsandbits2.api.render;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;

/**
 * Tracks what a given face of a chiseled block should look like.
 */
public interface FaceBuilder {
    /**
     * Sets the face that this face builder is building and the tint of said face.
     */
    void setFace(Direction myFace, int tintIndex);

    /**
     * Input data into the builder at a given element.
     */
    void put(int element, float... args);

    /**
     * Starts the builder.
     */
    void begin();

    /**
     * Creates a quad with a given sprite.
     */
    BakedQuad create(TextureAtlasSprite sprite);

    /**
     * Gets this faces vertex format.
     */
    VertexFormat getFormat();
}
