package nl.dgoossens.chiselsandbits2.client.render.chiseledblock;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import cpw.mods.modlauncher.Launcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.BlockFaceUV;
import net.minecraft.client.renderer.model.BlockPartFace;
import net.minecraft.client.renderer.model.BlockPartRotation;
import net.minecraft.client.renderer.model.FaceBakery;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfig;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockBaked;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.FormatInfo;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter.GfxRenderState;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter.RenderCache;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter.UploadTracker;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.NBTBlobConverter;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.ModelRenderState;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlobStateReference;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
import nl.dgoossens.chiselsandbits2.client.util.ModelUtil;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The general manager for the Chiseled Block TileEntityRenderers.
 */
public class RenderingManager {
    private final WeakHashMap<World, WorldTracker> worldTrackers = new WeakHashMap<>();
    private final Queue<TessellatorReferenceHolder> previousTessellators = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<VertexFormat, FormatInfo> formatData = new ConcurrentHashMap<>();
    private final VertexFormat VERTEX_FORMAT = new VertexFormat();
    private final ThreadPoolExecutor pool;
    private ChiseledBlockBaked NULL_MODEL;
    private float[][][] quadMapping;
    private int[][] faceVertMap;

    private final Cache<ModelRenderState, ChiseledBlockBaked> modelCache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();

    private final LoadingCache<ItemStack, IBakedModel> itemToModel = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(new CacheLoader<ItemStack, IBakedModel>() {
                @Override
                public IBakedModel load(ItemStack stack) throws Exception {
                    CompoundNBT c = stack.getTag();
                    if (c == null) c = new CompoundNBT();
                    else c = c.getCompound(ChiselUtil.NBT_BLOCKENTITYTAG);

                    byte[] vdata = c.getByteArray(NBTBlobConverter.NBT_VERSIONED_VOXEL);
                    VoxelBlobStateReference state = c.contains(NBTBlobConverter.NBT_VERSIONED_VOXEL) ? new VoxelBlobStateReference(vdata) : new VoxelBlobStateReference();

                    return getCachedModel(state, null, DefaultVertexFormats.ITEM);
                }
            });

    public RenderingManager() {
        //Initialise our rendering thread
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ThreadFactory threadFactory = (r) -> {
            final Thread t = new Thread(r);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.setName("C&B2 Render Thread");
            t.setContextClassLoader(classLoader);
            return t;
        };

        int processors = Runtime.getRuntime().availableProcessors();
        if (ChiselUtil.isLowMemoryMode()) processors = 1;
        pool = new ThreadPoolExecutor(1, processors, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), threadFactory);
        pool.allowCoreThreadTimeOut(false);

        //Build our vertex format with lightmap
        for (final VertexFormatElement element : DefaultVertexFormats.ITEM.getElements())
            VERTEX_FORMAT.addElement(element);
        VERTEX_FORMAT.addElement(DefaultVertexFormats.TEX_2S);
    }

    private void initialiseMaps() {
        faceVertMap = new int[6][4];
        quadMapping = new float[6][4][6];

        final Vector3f to = new Vector3f(0, 0, 0);
        final Vector3f from = new Vector3f(16, 16, 16);

        for (final Direction myFace : Direction.values()) {
            final FaceBakery faceBakery = new FaceBakery();

            final BlockPartRotation bpr = null;
            final ModelRotation mr = ModelRotation.X0_Y0;

            final float[] defUVs = new float[]{0, 0, 1, 1};
            final BlockFaceUV uv = new BlockFaceUV(defUVs, 0);
            final BlockPartFace bpf = new BlockPartFace(myFace, 0, "", uv);

            final BakedQuad q = faceBakery.makeBakedQuad(to, from, bpf, ChiselsAndBits2.getInstance().getClient().getMissingIcon(), myFace, mr, bpr, true);
            final int[] vertData = q.getVertexData();

            int a = 0;
            int b = 2;

            switch (myFace) {
                case NORTH:
                case SOUTH:
                    a = 0;
                    b = 1;
                    break;
                case EAST:
                case WEST:
                    a = 1;
                    b = 2;
                    break;
                default:
            }

            final int p = vertData.length / 4;
            for (int vertNum = 0; vertNum < 4; vertNum++) {
                final float A = Float.intBitsToFloat(vertData[vertNum * p + a]);
                final float B = Float.intBitsToFloat(vertData[vertNum * p + b]);

                for (int o = 0; o < 3; o++) {
                    final float v = Float.intBitsToFloat(vertData[vertNum * p + o]);
                    final float scaler = 1.0f / 16.0f; // pos start in the 0-16
                    quadMapping[myFace.ordinal()][vertNum][o * 2] = v * scaler;
                    quadMapping[myFace.ordinal()][vertNum][o * 2 + 1] = (1.0f - v) * scaler;
                }

                if (ModelUtil.isZero(A) && ModelUtil.isZero(B)) {
                    faceVertMap[myFace.getIndex()][vertNum] = 0;
                } else if (ModelUtil.isZero(A) && ModelUtil.isOne(B)) {
                    faceVertMap[myFace.getIndex()][vertNum] = 3;
                } else if (ModelUtil.isOne(A) && ModelUtil.isZero(B)) {
                    faceVertMap[myFace.getIndex()][vertNum] = 1;
                } else {
                    faceVertMap[myFace.getIndex()][vertNum] = 2;
                }
            }
        }
    }

    /**
     * Get or creates the model for a tile entity.
     */
    public ChiseledBlockBaked getCachedModel(final ChiseledBlockTileEntity te) {
        try {
            return getCachedModel(te.getVoxelReference(), te.getRenderTracker().getRenderState());
        } catch(ExecutionException x) {
            x.printStackTrace();
            return null;
        }
    }

    /**
     * Get our custom vertex format if we're using it.
     * @param checkPipeline Returns {@link DefaultVertexFormats#ITEM} if the forge light pipeline is disabled.
     */
    public VertexFormat getModelFormat(final boolean checkPipeline) {
        if(!checkPipeline) return VERTEX_FORMAT;
        return !ForgeConfig.CLIENT.forgeLightPipelineEnabled.get() ? DefaultVertexFormats.ITEM : VERTEX_FORMAT;
    }

    public boolean isInvalid(final ModelRenderState mrs) {
        //Invalidate this cache if the render tracker changed
        if (mrs != null && mrs.isDirty()) {
            modelCache.invalidate(mrs);
            return true;
        }
        return false;
    }

    public IBakedModel getItemModel(final ItemStack stack) throws ExecutionException {
        return itemToModel.get(stack);
    }

    public ChiseledBlockBaked getCachedModel(final VoxelBlobStateReference reference, final ModelRenderState mrs) throws ExecutionException {
        return getCachedModel(reference, mrs, getModelFormat(true));
    }

    public ChiseledBlockBaked getCachedModel(final VoxelBlobStateReference reference, final ModelRenderState mrs, final VertexFormat format) throws ExecutionException {
        if (reference == null) {
            if(NULL_MODEL == null) NULL_MODEL = new ChiseledBlockBaked(null, new ModelRenderState(), getModelFormat(true));
            return NULL_MODEL;
         }

        if (format == getModelFormat(true) && mrs != null)
            return modelCache.get(mrs, () -> new ChiseledBlockBaked(reference, mrs, format));

        return new ChiseledBlockBaked(reference, mrs, format);
    }

    public int getFaceVertexCount(int index, int vertex) {
        if(faceVertMap == null) initialiseMaps();
        return faceVertMap[index][vertex];
    }

    public float[] getQuadMapping(final Direction side, int vertex) {
        if(quadMapping == null) initialiseMaps();
        return quadMapping[side.ordinal()][vertex];
    }

    public FormatInfo getFormatInfo(final VertexFormat format) {
        return formatData.computeIfAbsent(format, FormatInfo::new);
    }

    /**
     * Submit an old tessellator for reuse.
     */
    public void submitTessellator(final Tessellator t) {
        previousTessellators.add(new TessellatorReferenceHolder(t));
    }

    /**
     * Get a tessellator to reuse that was previously
     */
    @Nullable
    public Tessellator getRecycledTessellator() {
        Tessellator tessellator = null;
        do {
            final TessellatorReferenceHolder holder = previousTessellators.poll();
            if (holder != null) {
                tessellator = holder.get();
                if (tessellator == null)
                    holder.dispose();
            }
        }
        while (tessellator == null && !previousTessellators.isEmpty());
        return tessellator;
    }

    /**
     * Get the maximal amount of tesselators that can be active simultaneously.
     */
    public int getMaxTesselators() {
        int dynamicTess = ChiselsAndBits2.getInstance().getConfig().dynamicMaxConcurrentTesselators.get();
        if (ChiselUtil.isLowMemoryMode()) dynamicTess = Math.min(2, dynamicTess);
        return dynamicTess;
    }

    /**
     * Get the worldtracker for the world the player is in.
     */
    public WorldTracker getTracker() {
        final World world = Minecraft.getInstance().player.world;
        if (!worldTrackers.containsKey(world))
            worldTrackers.put(world, new WorldTracker());
        return worldTrackers.get(world);
    }

    /**
     * Add a task to be executed on the next frame.
     */
    public void addNextFrameTask(final Runnable r) {
        getTracker().getNextFrameTasks().offer(r);
    }

    /**
     * Add a new future tracker for which rendering will be attempted to be
     * finalised every frame until it is finalised. (at which point it will be removed)
     */
    public void addFutureTracker(final RenderCache renderCache) {
        getTracker().getFutureTrackers().add(renderCache);
    }

    /**
     * Finalizes the rendering of a render cache. Only works if the render cache
     * has finished rendering in the background.
     */
    public boolean finalizeRendering(final RenderCache renderCache) {
        if (renderCache.hasRenderingCompleted()) {
            try {
                final Tessellator t = renderCache.getRenderingTask().get();
                getTracker().getUploaders().offer(new UploadTracker(renderCache, t));
            } catch (CancellationException cancel) { //We're fine if the future got cancelled.
            } catch (Exception x) {
                x.printStackTrace();
            } finally {
                renderCache.finishRendering();
            }
            return true;
        }
        if (!renderCache.isRendering())
            return true; //Remove if not rendering but not completed
        return false;
    }

    /**
     * Uploads all VBOs, finalises rendering for all future trackers.
     */
    public void uploadVBOs() {
        final WorldTracker tracker = getTracker();
        tracker.getFutureTrackers().removeIf(this::finalizeRendering);
        final Stopwatch w = Stopwatch.createStarted();
        do { //We always upload one, no matter how many ms you want us to do it for.
            final UploadTracker t = tracker.getUploaders().poll();
            if (t == null) return;
            uploadVBO(t);
        } while (w.elapsed(TimeUnit.MILLISECONDS) < ChiselsAndBits2.getInstance().getConfig().maxMillisecondsUploadingPerFrame.get());
    }

    /**
     * Uploads a given VBO.
     */
    public void uploadVBO(final UploadTracker t) {
        final Tessellator tx = t.getTessellator();
        if (t.getRenderCache().needsRebuilding())
            t.getRenderCache().setRenderState(GfxRenderState.getNewState(tx.getBuffer().getVertexCount()));

        if (t.getRenderCache().prepareRenderState(tx))
            t.submitForReuse();
    }

    /**
     * Runs all tasks in the queue one after another.
     */
    public void runJobs(final Queue<Runnable> tasks) {
        do {
            final Runnable x = tasks.poll();
            if (x == null) break;
            x.run();
        } while (true);
    }

    /**
     * Submit a task to the rendering thread.
     */
    public void submit(Runnable task) {
        pool.submit(task);
    }
}
