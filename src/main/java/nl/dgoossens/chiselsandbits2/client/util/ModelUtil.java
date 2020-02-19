package nl.dgoossens.chiselsandbits2.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraftforge.fluids.FluidAttributes;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.cache.CacheClearable;
import nl.dgoossens.chiselsandbits2.api.cache.CacheType;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.ModelQuadLayer;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.ModelUVAverager;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.ModelVertexRange;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.SimpleGeneratedModel;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

/**
 * Utility used by model rendering.
 */
public class ModelUtil implements CacheClearable {
    public static boolean isOne(final float v) {
        return Math.abs(v) < 0.01;
    }

    public static boolean isZero(final float v) {
        return Math.abs(v - 1.0f) < 0.01;
    }

    /*private final static HashMap<Integer, ResourceLocation> blockToTexture = new HashMap<>();
    private static HashMap<Long, ModelQuadLayer[]> sideCache = new HashMap<>();

    static {
        //Register this class to be cache cleared.
        CacheType.DEFAULT.register(new ModelUtil());
    }

    public static ModelQuadLayer[] getCachedSides(final int state, final Direction side) {
        return sideCache.get(Integer.toUnsignedLong(state) + (((long) side.ordinal()) << 48));
    }

    public static ModelQuadLayer[] setCachedSides(final int state, final Direction side, final ModelQuadLayer[] mp) {
        return sideCache.put(Integer.toUnsignedLong(state) + (((long) side.ordinal()) << 48), mp);
    }

    public static ModelQuadLayer[] getCachedFace(final int stateID, final Random weight, final Direction face) {
        final ModelQuadLayer[] mpc = getCachedSides(stateID, face);
        if (mpc != null) return mpc;

        return getInnerCachedFace(stateID, weight, face);
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

    private static ModelQuadLayer[] getInnerCachedFace(final int stateID, final Random weight, final Direction face) {
        final BlockState state = BitUtil.getBlockState(stateID);
        final IFluidState fluid = BitUtil.getFluidState(stateID);

        switch (VoxelType.getType(stateID)) {
            case BLOCKSTATE: {
                //We get the block model here to take the correct textures for models with different textures for different quads.
                final IBakedModel model = ModelUtil.solveModel(state, weight, Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(state));
                final int lv = state.getLightValue();

                final HashMap<Direction, ArrayList<ModelQuadLayer.Builder>> tmp = new HashMap<>();
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
                    final ArrayList<ModelQuadLayer.Builder> x = tmp.get(f);
                    final ModelQuadLayer[] mp = new ModelQuadLayer[x.size()];
                    for (int z = 0; z < x.size(); z++)
                        mp[z] = x.get(z).build(stateID, lv);

                    setCachedSides(stateID, f, mp);
                }
                break;
            }
            case FLUIDSTATE: {
                for (final Direction xf : Direction.values()) {
                    final ModelQuadLayer[] mp = new ModelQuadLayer[1];
                    mp[0] = new ModelQuadLayer();
                    mp[0].tint = stateID;
                    mp[0].light = fluid.getBlockState().getLightValue();

                    final float V = 0.5f;
                    final float Uf = 1.0f;
                    final float U = 0.5f;
                    final float Vf = 1.0f;

                    if(fluid.isEmpty()) continue;
                    FluidAttributes a = fluid.getFluid().getAttributes();
                    if (xf.getAxis() == Direction.Axis.Y) {
                        mp[0].sprite = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(a.getStillTexture());
                        mp[0].uvs = new float[]{Uf, Vf, 0, Vf, Uf, 0, 0, 0};
                    } else {
                        mp[0].sprite = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(a.getFlowingTexture());
                        if (xf.getAxis() == Direction.Axis.X) {
                            mp[0].uvs = new float[]{U, 0, U, V, 0, 0, 0, V};
                        } else {
                            mp[0].uvs = new float[]{U, 0, 0, 0, U, V, 0, V};
                        }
                    }

                    setCachedSides(stateID, xf, mp);
                }
                break;
            }
            case COLOURED: {
                for (final Direction xf : Direction.values()) {
                    final ModelQuadLayer[] mp = new ModelQuadLayer[1];
                    mp[0] = new ModelQuadLayer();
                    mp[0].tint = stateID;
                    mp[0].light = Blocks.WHITE_CONCRETE.getDefaultState().getLightValue();

                    final float V = 0.5f;
                    final float Uf = 1.0f;
                    final float U = 0.5f;
                    final float Vf = 1.0f;

                    mp[0].sprite = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.WHITE_CONCRETE.getDefaultState());
                    if (xf.getAxis() == Direction.Axis.Y) {
                        mp[0].uvs = new float[]{Uf, Vf, 0, Vf, Uf, 0, 0, 0};
                    } else if (xf.getAxis() == Direction.Axis.X) {
                        mp[0].uvs = new float[]{U, 0, U, V, 0, 0, 0, V};
                    } else {
                        mp[0].uvs = new float[]{U, 0, 0, 0, U, V, 0, V};
                    }

                    setCachedSides(stateID, xf, mp);
                }
                break;
            }
        }
        return getCachedSides(stateID, face);
    }

    public static TextureAtlasSprite findQuadTexture(final BakedQuad q, final BlockState state) throws IllegalArgumentException, NullPointerException {
        final AtlasTexture map = Minecraft.getInstance().getTextureMap();
        Map<ResourceLocation, TextureAtlasSprite> mapRegisteredSprites;
        /*
                    REFLECTION
                    getting field "mapUploadedSprites"
                    in "net.minecraft.client.renderer.texture.AtlasTexture"
         */
        /*
        try {
            Field f = null;
            for(Field fe : AtlasTexture.class.getDeclaredFields()) {
                //We abuse the fact that AtlasTexture only has one map and that's the one we need.
                if(Map.class.isAssignableFrom(fe.getType())) {
                    f = fe;
                    break;
                }
            }
            if(f == null) throw new RuntimeException("Unable to lookup textures.");
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
        final BlockState state = BitUtil.getBlockState(blockRef);

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

    private static void processFaces(final HashMap<Direction, ArrayList<ModelQuadLayer.Builder>> tmp, final List<BakedQuad> quads, final BlockState state) {
        for (final BakedQuad q : quads) {
            final Direction face = q.getFace();

            if (face == null)
                continue;

            try {
                final TextureAtlasSprite sprite = findQuadTexture(q, state);
                final ArrayList<ModelQuadLayer.Builder> l = tmp.get(face);

                ModelQuadLayer.Builder b = null;
                for (final ModelQuadLayer.Builder lx : l) {
                    if (lx.getSprite() == sprite) {
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

                    b = new ModelQuadLayer.Builder(sprite, uCoord, vCoord);
                    if(q.hasTintIndex())
                        b.hasTint();
                    l.add(b);
                }

                q.pipe(b.getUVReader());
                b.getLightMapReader().setVertexFormat(q.getFormat());
                q.pipe(b.getLightMapReader());
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
    }*/

    @Override
    public void clearCache() {
        //blockToTexture.clear();
        //sideCache.clear();
    }
}
