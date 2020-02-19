package nl.dgoossens.chiselsandbits2.client.render.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IDynamicBakedModel;

public abstract class BaseBakedModel implements IDynamicBakedModel, IForgeBakedModel {
    /*private static final Matrix4f ground;
    private static final Matrix4f gui;
    private static final Matrix4f fixed;
    private static final Matrix4f firstPerson_righthand;
    private static final Matrix4f firstPerson_lefthand;
    private static final Matrix4f thirdPerson_righthand;
    private static final Matrix4f thirdPerson_lefthand;

    static {
        gui = getMatrix(0, 0, 0, 30, 225, 0, 0.625f);
        ground = getMatrix(0, 3 / 16.0f, 0, 0, 0, 0, 0.25f);
        fixed = getMatrix(0, 0, 0, 0, 0, 0, 0.5f);
        thirdPerson_lefthand = thirdPerson_righthand = getMatrix(0, 2.5f / 16.0f, 0, 75, 45, 0, 0.375f);
        firstPerson_righthand = firstPerson_lefthand = getMatrix(0, 0, 0, 0, 45, 0, 0.40f);
    }

    private static Matrix4f getMatrix(final float transX, final float transY, final float transZ, final float rotX, final float rotY, final float rotZ, final float scaleXYZ) {
        final javax.vecmath.Vector3f translation = new javax.vecmath.Vector3f(transX, transY, transZ);
        final javax.vecmath.Vector3f scale = new javax.vecmath.Vector3f(scaleXYZ, scaleXYZ, scaleXYZ);
        final Quat4f rotation = TRSRTransformation.quatFromXYZDegrees(new javax.vecmath.Vector3f(rotX, rotY, rotZ));

        final TRSRTransformation transform = new TRSRTransformation(translation, rotation, scale, null);
        return transform.getMatrixVec();
    }*/

    @Override
    public boolean func_230044_c_() {
        return true; //Is block item? (false = flat lightning, true = 3d lightning)
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType cameraTransformType, MatrixStack mat) {
        return null;
    }

    /*@Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(final ItemCameraTransforms.TransformType cameraTransformType) {
        switch (cameraTransformType) {
            case FIRST_PERSON_LEFT_HAND:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, firstPerson_lefthand);
            case FIRST_PERSON_RIGHT_HAND:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, firstPerson_righthand);
            case THIRD_PERSON_LEFT_HAND:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, thirdPerson_lefthand);
            case THIRD_PERSON_RIGHT_HAND:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, thirdPerson_righthand);
            case FIXED:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, fixed);
            case GROUND:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, ground);
            case GUI:
                return new ImmutablePair<IBakedModel, Matrix4f>(this, gui);
            default:
        }
        return new ImmutablePair<IBakedModel, Matrix4f>(this, fixed);
    }*/

    @Override
    final public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    final public boolean isGui3d() {
        return true;
    }

    @Override
    final public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    final public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.EMPTY;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null;
    }
}
