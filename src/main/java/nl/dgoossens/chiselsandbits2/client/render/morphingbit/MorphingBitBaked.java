package nl.dgoossens.chiselsandbits2.client.render.morphingbit;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.BlockFaceUV;
import net.minecraft.client.renderer.model.BlockPartFace;
import net.minecraft.client.renderer.model.BlockPartRotation;
import net.minecraft.client.renderer.model.FaceBakery;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.util.Direction;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import nl.dgoossens.chiselsandbits2.client.render.model.BaseBakedModel;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.ModelQuadLayer;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.client.util.ModelUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MorphingBitBaked extends BaseBakedModel implements IDynamicBakedModel, IForgeBakedModel {
    private static final float PIXELS_PER_BLOCK = 16.0f;
    private static final float BIT_BEGIN = 4.0f;
    private static final float BIT_WIDTH = 8.0f;
    private static final float Y_BIT_BEGIN = 2.0f;

    private static final float BIT_END = BIT_BEGIN + BIT_WIDTH;
    private static final float Y_BIT_END = Y_BIT_BEGIN + BIT_WIDTH;

    private final ArrayList<BakedQuad> generic = new ArrayList<>(6);
    
    /**
     * Creates a new morphing bit baked model for a given bit id.
     */
    public MorphingBitBaked(int bitId, boolean big) {
        //If this is AIR, use a nice transparent gray bit colour.
        if(bitId == VoxelBlob.AIR_BIT)
            bitId = BitUtil.getColourId(new Color(86, 86, 86, 175));

        final FaceBakery faceBakery = new FaceBakery();

        final Vector3f to = big ? new Vector3f(0, 0, 0) : new Vector3f(BIT_BEGIN, Y_BIT_BEGIN, BIT_BEGIN);
        final Vector3f from = big ? new Vector3f(16, 16 ,16) : new Vector3f(BIT_END, Y_BIT_END, BIT_END);

        final BlockPartRotation bpr = null;
        final ModelRotation mr = ModelRotation.X0_Y0;

        Random rand = new Random();
        for (final Direction myFace : Direction.values()) {
            //We use the shadeBitId to make it look shaded.
            final ModelQuadLayer[] layers = null; //TODO ModelUtil.getCachedFace(bitId, rand, myFace);

            if (layers == null || layers.length == 0)
                continue;

            for (final ModelQuadLayer clayer : layers) {
                final BlockFaceUV uv = new BlockFaceUV(getFaceUvs(myFace, big), 0);
                final BlockPartFace bpf = new BlockPartFace(myFace, clayer.tint, "", uv);

                Vector3f toB, fromB;
                switch (myFace) {
                    case UP:
                        toB = new Vector3f(to.getX(), from.getY(), to.getZ());
                        fromB = new Vector3f(from.getX(), from.getY(), from.getZ());
                        break;
                    case EAST:
                        toB = new Vector3f(from.getX(), to.getY(), to.getZ());
                        fromB = new Vector3f(from.getX(), from.getY(), from.getZ());
                        break;
                    case NORTH:
                        toB = new Vector3f(to.getX(), to.getY(), to.getZ());
                        fromB = new Vector3f(from.getX(), from.getY(), to.getZ());
                        break;
                    case SOUTH:
                        toB = new Vector3f(to.getX(), to.getY(), from.getZ());
                        fromB = new Vector3f(from.getX(), from.getY(), from.getZ());
                        break;
                    case DOWN:
                        toB = new Vector3f(to.getX(), to.getY(), to.getZ());
                        fromB = new Vector3f(from.getX(), to.getY(), from.getZ());
                        break;
                    case WEST:
                        toB = new Vector3f(to.getX(), to.getY(), to.getZ());
                        fromB = new Vector3f(to.getX(), from.getY(), from.getZ());
                        break;
                    default:
                        throw new NullPointerException();
                }

                generic.add(faceBakery.bakeQuad(toB, fromB, bpf, clayer.sprite, myFace, mr, bpr, true, MissingTextureSprite.getLocation())); //TODO fix
            }
        }

        generic.trimToSize();
    }

    private float[] getFaceUvs(final Direction face, final boolean big) {
        float[] afloat;

        final int from_x = big ? 0 : (int) BIT_BEGIN;
        final int from_y = big ? 0 : (int) BIT_BEGIN;
        final int from_z = big ? 0 : (int) BIT_BEGIN;

        final int to_x = big ? 16 : (int) BIT_END;
        final int to_y = big ? 16 : (int) BIT_END;
        final int to_z = big ? 16 : (int) BIT_END;

        switch (face) {
            case DOWN:
            case UP:
                afloat = new float[]{from_x, from_z, to_x, to_z};
                break;
            case NORTH:
            case SOUTH:
                afloat = new float[]{from_x, PIXELS_PER_BLOCK - to_y, to_x, PIXELS_PER_BLOCK - from_y};
                break;
            case WEST:
            case EAST:
                afloat = new float[]{from_z, PIXELS_PER_BLOCK - to_y, to_z, PIXELS_PER_BLOCK - from_y};
                break;
            default:
                throw new NullPointerException();
        }

        return afloat;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        if (side != null) return Collections.emptyList();
        return generic;
    }
}
