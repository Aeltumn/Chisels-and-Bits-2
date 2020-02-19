package nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.ForgeConfig;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.render.ICullTest;
import nl.dgoossens.chiselsandbits2.api.render.IFaceBuilder;
import nl.dgoossens.chiselsandbits2.api.render.IStateRef;
import nl.dgoossens.chiselsandbits2.client.cull.MCCullTest;
import nl.dgoossens.chiselsandbits2.client.render.model.BaseBakedModel;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.ModelRenderState;
import nl.dgoossens.chiselsandbits2.client.render.model.helpers.ModelQuadLayer;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlobStateReference;
import nl.dgoossens.chiselsandbits2.client.util.ModelUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ChiseledBlockBaked extends BaseBakedModel implements IDynamicBakedModel, IForgeBakedModel {
    private BakedQuad[] up;
    private BakedQuad[] down;
    private BakedQuad[] north;
    private BakedQuad[] south;
    private BakedQuad[] east;
    private BakedQuad[] west;
    private BakedQuad[] generic;

    public ChiseledBlockBaked(final VoxelBlobStateReference data, final ModelRenderState mrs) {
        if (data == null) return;

        final VoxelBlob vb = data.getVoxelBlob();
        final ChiseledModelBuilder builder = new ChiseledModelBuilder();
        generateFaces(builder, vb, mrs);

        // convert from builder to final storage.
        up = builder.getSide(Direction.UP);
        down = builder.getSide(Direction.DOWN);
        east = builder.getSide(Direction.EAST);
        west = builder.getSide(Direction.WEST);
        north = builder.getSide(Direction.NORTH);
        south = builder.getSide(Direction.SOUTH);
        generic = builder.getSide(null);
    }



    private static void offsetVec(final int[] result, final int toX, final int toY, final int toZ, final Direction f, final int d) {
        int leftX = 0;
        final int leftY = 0;
        int leftZ = 0;

        final int upX = 0;
        int upY = 0;
        int upZ = 0;

        switch (f) {
            case DOWN:
                leftX = 1;
                upZ = 1;
                break;
            case EAST:
                leftZ = 1;
                upY = 1;
                break;
            case NORTH:
                leftX = 1;
                upY = 1;
                break;
            case SOUTH:
                leftX = 1;
                upY = 1;
                break;
            case UP:
                leftX = 1;
                upZ = 1;
                break;
            case WEST:
                leftZ = 1;
                upY = 1;
                break;
            default:
                break;
        }

        result[0] = (toX + leftX * d + upX * d) / 2;
        result[1] = (toY + leftY * d + upY * d) / 2;
        result[2] = (toZ + leftZ * d + upZ * d) / 2;
    }

    private List<BakedQuad> getList(final Direction side) {
        if (side != null) {
            switch (side) {
                case DOWN:
                    return asList(down);
                case EAST:
                    return asList(east);
                case NORTH:
                    return asList(north);
                case SOUTH:
                    return asList(south);
                case UP:
                    return asList(up);
                case WEST:
                    return asList(west);
            }
        }
        return asList(generic);
    }

    private List<BakedQuad> asList(final BakedQuad[] array) {
        if (array == null) return Collections.emptyList();
        return Arrays.asList(array);
    }

    public boolean isEmpty() {
        if (!getList(null).isEmpty()) return false;
        for (final Direction e : Direction.values()) {
            if (!getList(e).isEmpty()) return false;
        }
        return true;
    }

    private void generateFaces(final ChiseledModelBuilder builder, final VoxelBlob blob, final ModelRenderState mrs) {
        final ArrayList<ArrayList<FaceRegion>> rset = new ArrayList<>();
        final VoxelBlob.VisibleFace visFace = new VoxelBlob.VisibleFace();

        for (Direction direction : Direction.values())
            processFace(blob, visFace, mrs, rset, direction);

        // re-usable float[]'s to minimize garbage cleanup.
        final int[] to = new int[3];
        final int[] from = new int[3];
        final float[] uvs = new float[8];
        final float[] pos = new float[3];

        // single reusable face builder.
        final IFaceBuilder faceBuilder = new ChiselsAndBitsBakedQuad.Builder(DefaultVertexFormats.BLOCK);
        for (final ArrayList<FaceRegion> src : rset) {
            mergeFaces(src);

            for (final FaceRegion region : src) {
                final Direction myFace = region.getFace();

                // keep integers up until the last moment... ( note I tested
                // snapping the floats after this stage, it made no
                // difference. )
                offsetVec(to, region.getMaxX(), region.getMaxY(), region.getMaxZ(), myFace, 1);
                offsetVec(from, region.getMinX(), region.getMinY(), region.getMinZ(), myFace, -1);
                final ModelQuadLayer[] mpc = null; //TODO ModelUtil.getCachedFace(region.getState(), new Random(), myFace);

                if (mpc != null) {
                    for (final ModelQuadLayer pc : mpc) {
                        VertexFormat builderFormat = faceBuilder.getFormat();

                        faceBuilder.begin();
                        faceBuilder.setFace(myFace, pc.tint);

                        final float maxLightmap = 32.0f / 0xffff;
                        getFaceUvs(uvs, myFace, from, to, pc.uvs);

                        // build it.
                        for (int vertNum = 0; vertNum < 4; vertNum++) {
                            for (int elementIndex = 0; elementIndex < builderFormat.getElements().size(); elementIndex++) {
                                final VertexFormatElement element = builderFormat.getElements().get(elementIndex);
                                switch (element.getUsage()) {
                                    case POSITION:
                                        getVertexPos(pos, myFace, vertNum, to, from);
                                        faceBuilder.put(elementIndex, pos[0], pos[1], pos[2]);
                                        break;

                                    case COLOR:
                                        faceBuilder.put(elementIndex, 1.0f, 1.0f, 1.0f, 1.0f); //We do no recoloring, let the BlockColor handle it.
                                        break;

                                    case NORMAL:
                                        // this fixes a bug with Forge AO?? and
                                        // solid blocks.. I have no idea why...
                                        final float normalShift = 0.999f;
                                        faceBuilder.put(elementIndex, normalShift * myFace.getXOffset(), normalShift * myFace.getYOffset(), normalShift * myFace.getZOffset());
                                        break;

                                    case UV:
                                        if (element.getIndex() == 1) {
                                            final float v = maxLightmap * Math.max(0, Math.min(15, pc.light));
                                            faceBuilder.put(elementIndex, v, v);
                                        } else {
                                            int i = ChiselsAndBits2.getInstance().getClient().getRenderingManager().getFaceVertexCount(myFace.getIndex(), vertNum) * 2;
                                            final float u = uvs[i];
                                            final float v = uvs[i + 1];
                                            faceBuilder.put(elementIndex, pc.sprite.getInterpolatedU(u), pc.sprite.getInterpolatedV(v));
                                        }
                                        break;

                                    default:
                                        faceBuilder.put(elementIndex);
                                        break;
                                }
                            }
                        }

                        if (region.isEdge())
                            builder.getList(myFace).add(faceBuilder.create(pc.sprite));
                        else
                            builder.getList(null).add(faceBuilder.create(pc.sprite));
                    }
                }
            }
        }
    }

    private float notZero(float byteToFloat) {
        if (byteToFloat < 0.00001f)
            return 1;
        return byteToFloat;
    }

    private float byteToFloat(final int i) {
        return (i & 0xff) / 255.0f;
    }

    private void mergeFaces(final ArrayList<FaceRegion> src) {
        boolean restart;

        do {
            restart = false;

            final int size = src.size();
            final int sizeMinusOne = size - 1;

            restart:
            for (int x = 0; x < sizeMinusOne; ++x) {
                final FaceRegion faceA = src.get(x);

                for (int y = x + 1; y < size; ++y) {
                    final FaceRegion faceB = src.get(y);

                    if (faceA.extend(faceB)) {
                        src.set(y, src.get(sizeMinusOne));
                        src.remove(sizeMinusOne);

                        restart = true;
                        break restart;
                    }
                }
            }
        }
        while (restart);
    }

    private void processFace(final VoxelBlob blob, final VoxelBlob.VisibleFace visFace, final ModelRenderState mrs, final ArrayList<ArrayList<FaceRegion>> rset, final Direction myFace) {
        ArrayList<FaceRegion> regions = null;
        final ICullTest test = new MCCullTest();
        final IStateRef nextToState = mrs != null ? mrs.get(myFace) : null;
        VoxelBlob nextTo = nextToState == null ? null : nextToState.getVoxelBlob();

        //Z: z, y, x
        //Y: y, z, x
        //X: x, z, y
        for (int first = 0; first < VoxelBlob.DIMENSION; first++) {
            if (regions == null)
                regions = new ArrayList<>(16);

            for (int second = 0; second < VoxelBlob.DIMENSION; second++) {
                FaceRegion currentFace = null;

                for (int third = 0; third < VoxelBlob.DIMENSION; third++) {
                    FaceRegion region;
                    if (myFace == Direction.NORTH || myFace == Direction.SOUTH) //Z faces
                        region = getRegion(blob, myFace, third, second, first, visFace, nextTo, test);
                    else if (myFace == Direction.UP || myFace == Direction.DOWN) //Y faces
                        region = getRegion(blob, myFace, third, first, second, visFace, nextTo, test);
                    else //X faces
                        region = getRegion(blob, myFace, first, third, second, visFace, nextTo, test);

                    if (region == null) {
                        currentFace = null;
                        continue;
                    }

                    if (currentFace != null) {
                        if (currentFace.extend(region)) {
                            continue;
                        }
                    }

                    currentFace = region;
                    regions.add(region);
                }
            }

            if (!regions.isEmpty()) {
                rset.add(regions);
                regions = null;
            }
        }
    }

    private FaceRegion getRegion(final VoxelBlob blob, final Direction myFace, final int x, final int y, final int z, final VoxelBlob.VisibleFace visFace, final VoxelBlob nextTo, final ICullTest test) {
        blob.updateFaceVisibility(myFace, x, y, z, visFace, nextTo, test);

        if (visFace.visibleFace) {
            final Vec3i off = myFace.getDirectionVec();
            return new FaceRegion(myFace, x * 2 + 1 + off.getX(), y * 2 + 1 + off.getY(), z * 2 + 1 + off.getZ(), visFace.state, visFace.isEdge);
        }
        return null;
    }

    // generate final pos from static data.
    private void getVertexPos(final float[] pos, final Direction side, final int vertNum, final int[] to, final int[] from) {
        final float[] interpos = ChiselsAndBits2.getInstance().getClient().getRenderingManager().getQuadMapping(side, vertNum);

        pos[0] = to[0] * interpos[0] + from[0] * interpos[1];
        pos[1] = to[1] * interpos[2] + from[1] * interpos[3];
        pos[2] = to[2] * interpos[4] + from[2] * interpos[5];
    }

    private void getFaceUvs(final float[] uvs, final Direction face, final int[] from, final int[] to, final float[] quadsUV) {
        float to_u = 0;
        float to_v = 0;
        float from_u = 0;
        float from_v = 0;

        switch (face) {
            case UP:
            case DOWN:
                to_u = to[0] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            case SOUTH:
            case NORTH:
                to_u = to[0] / 16.0f;
                to_v = to[1] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[1] / 16.0f;
                break;
            case EAST:
            case WEST:
                to_u = to[1] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[1] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            default:
        }

        uvs[0] = 16.0f * u(quadsUV, to_u, to_v); // 0
        uvs[1] = 16.0f * v(quadsUV, to_u, to_v); // 1

        uvs[2] = 16.0f * u(quadsUV, from_u, to_v); // 2
        uvs[3] = 16.0f * v(quadsUV, from_u, to_v); // 3

        uvs[4] = 16.0f * u(quadsUV, from_u, from_v); // 2
        uvs[5] = 16.0f * v(quadsUV, from_u, from_v); // 3

        uvs[6] = 16.0f * u(quadsUV, to_u, from_v); // 0
        uvs[7] = 16.0f * v(quadsUV, to_u, from_v); // 1
    }

    private float u(final float[] src, final float inU, final float inV) {
        final float inv = 1.0f - inU;
        final float u1 = src[0] * inU + inv * src[2];
        final float u2 = src[4] * inU + inv * src[6];
        return u1 * inV + (1.0f - inV) * u2;
    }

    private float v(final float[] src, final float inU, final float inV) {
        final float inv = 1.0f - inU;
        final float v1 = src[1] * inU + inv * src[3];
        final float v2 = src[5] * inU + inv * src[7];
        return v1 * inV + (1.0f - inV) * v2;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        return getList(side);
    }
}
