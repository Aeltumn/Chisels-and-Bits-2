package nl.dgoossens.chiselsandbits2.common.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.VoxelType;
import nl.dgoossens.chiselsandbits2.client.render.models.CacheClearable;
import nl.dgoossens.chiselsandbits2.client.render.models.helpers.ModelQuadLayer;
import nl.dgoossens.chiselsandbits2.client.render.models.helpers.ModelUVAverager;
import nl.dgoossens.chiselsandbits2.client.render.models.helpers.ModelVertexRange;
import nl.dgoossens.chiselsandbits2.client.render.models.helpers.SimpleGeneratedModel;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

public class ModelUtil implements CacheClearable {
    private final static HashMap<Integer, ResourceLocation> blockToTexture = new HashMap<>();
    private static HashMap<Integer, ModelQuadLayer[]> cache = new HashMap<>();
    private static HashMap<Integer, TextureAtlasSprite> breakCache = new HashMap<>();

    public static boolean isOne(final float v) {
        return Math.abs(v) < 0.01;
    }

    public static boolean isZero(final float v) {
        return Math.abs(v - 1.0f) < 0.01;
    }

    public static ModelQuadLayer[] getCachedFace(final int stateID, final Random weight, final Direction face) {
        final int cacheVal = stateID << 4 | face.ordinal();
        final ModelQuadLayer[] mpc = cache.get(cacheVal);
        if (mpc != null) return mpc;

        return getInnerCachedFace(cacheVal, stateID, weight, face);
    }

    public static IBakedModel solveModel(final BlockState state, final Random weight, final IBakedModel originalModel) {
        boolean hasFaces = false;

        try {
            hasFaces = hasFaces(originalModel, state, null, weight);

            for (final Direction f : Direction.values())
                hasFaces = hasFaces || hasFaces(originalModel, state, f, weight);
        } catch (final Exception e) {
            // an exception was thrown.. use the item model and hope...
            hasFaces = false;
        }

        if (!hasFaces) {
            // if the model is empty then lets grab an item and try that...
            final ItemStack is = new ItemStack(state.getBlock());
            if (!is.isEmpty()) {
                final IBakedModel itemModel = Minecraft.getInstance().getItemRenderer().getItemModelWithOverrides(is, Minecraft.getInstance().world, Minecraft.getInstance().player);

                try {
                    hasFaces = hasFaces(itemModel, state, null, weight);

                    for (final Direction f : Direction.values())
                        hasFaces = hasFaces || hasFaces(itemModel, state, f, weight);
                } catch (final Exception e) {
                    // an exception was thrown.. use the item model and hope...
                    hasFaces = false;
                }

                if (hasFaces) return itemModel;
                else return new SimpleGeneratedModel(findTexture(Block.getStateId(state), originalModel, Direction.UP));
            }
        }

        return originalModel;
    }

    private static boolean hasFaces(final IBakedModel model, final BlockState state, final Direction f, final Random weight) {
        final List<BakedQuad> l = getModelQuads(model, state, f, weight);
        if (l == null || l.isEmpty()) return false;

        TextureAtlasSprite texture = null;
        try {
            texture = findTexture(null, l, f);
        } catch (final Exception e) {}

        final ModelVertexRange mvr = new ModelVertexRange();
        for (final BakedQuad q : l)
            q.pipe(mvr);

        return mvr.getLargestRange() > 0 && !isMissing(texture);
    }

    private static ModelQuadLayer[] getInnerCachedFace(final int cacheVal, final int stateID, final Random weight, final Direction face) {
        final BlockState state = ModUtil.getBlockState(stateID);
        final IFluidState fluid = ModUtil.getFluidState(stateID);
        final Color colour = ModUtil.getColourState(stateID);

        switch (VoxelType.getType(stateID)) {
            case BLOCKSTATE: {
                final IBakedModel model = ModelUtil.solveModel(state, weight, Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(state));
                final int lv = state.getLightValue();

                final HashMap<Direction, ArrayList<ModelQuadLayer.ModelQuadLayerBuilder>> tmp = new HashMap<>();

                for (final Direction f : Direction.values())
                    tmp.put(f, new ArrayList<>());

                if (model != null) {
                    for (final Direction f : Direction.values()) {
                        final List<BakedQuad> quads = ModelUtil.getModelQuads(model, state, f, new Random());
                        processFaces(tmp, quads, state);
                    }

                    processFaces(tmp, ModelUtil.getModelQuads(model, state, null, new Random()), state);
                }

                for (final Direction f : Direction.values()) {
                    final int cacheV = stateID << 4 | f.ordinal();
                    final ArrayList<ModelQuadLayer.ModelQuadLayerBuilder> x = tmp.get(f);
                    final ModelQuadLayer[] mp = new ModelQuadLayer[x.size()];

                    for (int z = 0; z < x.size(); z++) {
                        mp[z] = x.get(z).build(stateID, 0xffffff, lv);
                    }

                    cache.put(cacheV, mp);
                }

                return cache.get(cacheVal);
            }
            case FLUIDSTATE: {
                for (final Direction xf : Direction.values()) {
                    final ModelQuadLayer[] mp = new ModelQuadLayer[1];
                    mp[0] = new ModelQuadLayer();
                    mp[0].color = 0xffffff;
                    mp[0].light = fluid.getBlockState().getLightValue();

                    final float V = 0.5f;
                    final float Uf = 1.0f;
                    final float U = 0.5f;
                    final float Vf = 1.0f;


                    mp[0].sprite = Minecraft.getInstance().getTextureMap().getAtlasSprite(fluid.getFluid().toString());
                    if (xf.getAxis() == Direction.Axis.Y) {
                        mp[0].uvs = new float[]{Uf, Vf, 0, Vf, Uf, 0, 0, 0};
                    } else if (xf.getAxis() == Direction.Axis.X) {
                        mp[0].uvs = new float[]{U, 0, U, V, 0, 0, 0, V};
                    } else {
                        mp[0].uvs = new float[]{U, 0, 0, 0, U, V, 0, V};
                    }
                    mp[0].tint = 0;

                    final int cacheV = stateID << 4 | xf.ordinal();
                    cache.put(cacheV, mp);
                }
                return cache.get(cacheVal);
            }
            case COLOURED: {

                return cache.get(cacheVal);
            }
        }
        return cache.get(cacheVal);
    }

    public static TextureAtlasSprite findQuadTexture(final BakedQuad q, final BlockState state) throws IllegalArgumentException, NullPointerException {
        final AtlasTexture map = Minecraft.getInstance().getTextureMap();
        Map<ResourceLocation, TextureAtlasSprite> mapRegisteredSprites;
        /*
                    REFLECTION
                    getting field "mapUploadedSprites"
                    in "net.minecraft.client.renderer.texture.AtlasTexture"
         */
        try {
            Field f = map.getClass().getDeclaredField("mapUploadedSprites");
            f.setAccessible(true);
            mapRegisteredSprites = (Map<ResourceLocation, TextureAtlasSprite>) f.get(map);
        } catch (Exception x) {
            x.printStackTrace();
            throw new RuntimeException("Unable to lookup textures.");
        }

        if (mapRegisteredSprites == null) {
            throw new RuntimeException("Unable to lookup textures.");
        }

        final ModelUVAverager av = new ModelUVAverager();
        q.pipe(av);

        final float U = av.getU();
        final float V = av.getV();

        final Iterator<?> iterator1 = mapRegisteredSprites.values().iterator();
        while (iterator1.hasNext()) {
            final TextureAtlasSprite sprite = (TextureAtlasSprite) iterator1.next();
            if (sprite.getMinU() <= U && U <= sprite.getMaxU() && sprite.getMinV() <= V && V <= sprite.getMaxV()) {
                return sprite;
            }
        }

        TextureAtlasSprite texture = null;

        try {
            if (q.getSprite() != null) {
                texture = q.getSprite();
            }
        } catch (final Exception e) {
        }

        if (isMissing(texture) && state != null) {
            try {
                texture = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
            } catch (final Exception err) {
            }
        }

        if (texture == null)
            return ChiselsAndBits2.getInstance().getClient().getMissingIcon();

        return texture;
    }

    public static TextureAtlasSprite findTexture(final int blockRef, final IBakedModel model, final Direction myFace) {
        // didn't work? ok lets try scanning for the texture in the
        if (blockToTexture.containsKey(blockRef)) {
            final ResourceLocation textureName = blockToTexture.get(blockRef);
            return (TextureAtlasSprite) Minecraft.getInstance().getTextureManager().getTexture(textureName);
        }

        TextureAtlasSprite texture = null;
        final BlockState state = ModUtil.getBlockState(blockRef);

        if (model != null) {
            try {
                texture = findTexture(texture, getModelQuads(model, state, myFace, new Random()), myFace);

                if (texture == null) {
                    for (final Direction side : Direction.values())
                        texture = findTexture(texture, getModelQuads(model, state, side, new Random()), side);

                    texture = findTexture(texture, getModelQuads(model, state, null, new Random()), null);
                }
            } catch (final Exception errr) {}
        }

        // who knows if that worked.. now lets try to fromName a texture...
        if (isMissing(texture)) {
            try {
                if (model != null)
                    texture = model.getParticleTexture();
            } catch (final Exception err) {}
        }

        if (isMissing(texture)) {
            try {
                texture = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
            } catch (final Exception err) {}
        }

        if (texture == null)
            texture = ChiselsAndBits2.getInstance().getClient().getMissingIcon();

        blockToTexture.put(blockRef, texture.getName());
        return texture;
    }

    private static boolean isMissing(final TextureAtlasSprite texture) {
        return texture == null || texture == ChiselsAndBits2.getInstance().getClient().getMissingIcon();
    }

    private static TextureAtlasSprite findTexture(TextureAtlasSprite texture, final List<BakedQuad> faceQuads, final Direction myFace) throws IllegalArgumentException, NullPointerException {
        for (final BakedQuad q : faceQuads) {
            if (q.getFace() == myFace)
                texture = findQuadTexture(q, null);
        }
        return texture;
    }

    public static TextureAtlasSprite getBreakingTexture(int blockStateId) {
        TextureAtlasSprite out = breakCache.get(blockStateId);

        if (out == null && VoxelType.getType(blockStateId) == VoxelType.BLOCKSTATE) {
            final BlockState state = ModUtil.getBlockState(blockStateId);
            final IBakedModel model = ModelUtil.solveModel(state, new Random(), Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(ModUtil.getBlockState(blockStateId)));

            if (model != null)
                out = ModelUtil.findTexture(blockStateId, model, Direction.UP);

            breakCache.put(blockStateId, out);
        }

        return out;
    }

    private static void processFaces(final HashMap<Direction, ArrayList<ModelQuadLayer.ModelQuadLayerBuilder>> tmp, final List<BakedQuad> quads, final BlockState state) {
        for (final BakedQuad q : quads) {
            final Direction face = q.getFace();

            if (face == null)
                continue;

            try {
                final TextureAtlasSprite sprite = findQuadTexture(q, state);
                final ArrayList<ModelQuadLayer.ModelQuadLayerBuilder> l = tmp.get(face);

                ModelQuadLayer.ModelQuadLayerBuilder b = null;
                for (final ModelQuadLayer.ModelQuadLayerBuilder lx : l) {
                    if (lx.cache.sprite == sprite) {
                        b = lx;
                        break;
                    }
                }

                if (b == null) {
                    // top/bottom
                    int uCoord = 0;
                    int vCoord = 2;

                    switch (face) {
                        case NORTH:
                        case SOUTH:
                            uCoord = 0;
                            vCoord = 1;
                            break;
                        case EAST:
                        case WEST:
                            uCoord = 1;
                            vCoord = 2;
                            break;
                        default:
                    }

                    b = new ModelQuadLayer.ModelQuadLayerBuilder(sprite, uCoord, vCoord);
                    b.cache.tint = q.getTintIndex();
                    l.add(b);
                }

                q.pipe(b.uvr);
                b.lv.setVertexFormat(q.getFormat());
                q.pipe(b.lv);
            } catch (final Exception e) {}
        }
    }

    private static List<BakedQuad> getModelQuads(final IBakedModel model, final BlockState state, final Direction f, final Random rand) {
        try {
            // try to fromName block model...
            return model.getQuads(state, f, rand);
        } catch (final Throwable t) { }

        try {
            // try to fromName item model?
            return model.getQuads(null, f, rand);
        } catch (final Throwable t) { }

        final ItemStack is = new ItemStack(state.getBlock());
        if (is.isEmpty()) {
            final IBakedModel secondModel = getOverrides(model).getModelWithOverrides(model, is, Minecraft.getInstance().world, Minecraft.getInstance().player);

            if (secondModel != null) {
                try {
                    return secondModel.getQuads(null, f, rand);
                } catch (final Throwable t) {}
            }
        }

        // try to not crash...
        return Collections.emptyList();
    }

    private static ItemOverrideList getOverrides(final IBakedModel model) {
        if (model != null) {
            final ItemOverrideList modelOverrides = model.getOverrides();
            return modelOverrides == null ? ItemOverrideList.EMPTY : modelOverrides;
        }
        return ItemOverrideList.EMPTY;
    }

    @Override
    public void clearCache() {
        blockToTexture.clear();
        cache.clear();
        breakCache.clear();
    }
}
