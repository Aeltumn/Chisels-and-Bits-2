package nl.dgoossens.chiselsandbits2.client.render.model;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class BaseSmartModel implements IDynamicBakedModel, IForgeBakedModel {
    private final ItemOverrideList overrides;
    private static final IBakedModel NULL_MODEL = new IBakedModel() {
        @Override
        public List<BakedQuad> getQuads(final BlockState state, final Direction side, final Random rand, @Nonnull final IModelData modelData) {
            return getQuads(state, side, rand);
        }

        @Override
        public List<BakedQuad> getQuads(final BlockState state, final Direction side, final Random rand) {
            return Collections.emptyList();
        }

        @Override
        public boolean isAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean func_230044_c_() {
            return true; //3d lightning
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return null; //TODO missing icon?
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.EMPTY;
        }
    };

    public BaseSmartModel() {
        overrides = new OverrideHelper(this);
    }

    /**
     * Handle the model when rendered for a block.
     */
    public IBakedModel handleBlockState(final BlockState state, final Random rand, @Nonnull final IModelData modelData) {
        return NULL_MODEL;
    }

    /**
     * Handle the model when it is rendered by an item.
     */
    public IBakedModel handleItemState(final IBakedModel originalModel, final ItemStack stack, final World world, final LivingEntity entity) {
        return originalModel;
    }

    @Override
    public boolean func_230044_c_() {
        return true; //3d lightning
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data) {
        return getParticleTexture();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null; //TODO missing icon
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        final IBakedModel model = handleBlockState(state, rand, extraData);
        return model.getQuads(state, side, rand, extraData);
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    private static class OverrideHelper extends ItemOverrideList {
        final BaseSmartModel parent;

        public OverrideHelper(final BaseSmartModel p) {
            super();
            parent = p;
        }

        @Nullable
        public IBakedModel getModelWithOverrides(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable LivingEntity entity) {
            return parent.handleItemState(originalModel, stack, world, entity);
        }
    }
}
