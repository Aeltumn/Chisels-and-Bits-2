package nl.dgoossens.chiselsandbits2.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.block.BitOperation;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IBitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.IItemMenu;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.radial.RadialMenu;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.RenderingManager;
import nl.dgoossens.chiselsandbits2.api.item.IMenuAction;
import nl.dgoossens.chiselsandbits2.client.render.RenderingAssistant;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.NBTBlobConverter;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel.ChiselIterator;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.chisel.ChiselTypeIterator;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.BlockPlacementLogic;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.VoxelRegionSrc;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.items.ChiseledBlockItem;
import nl.dgoossens.chiselsandbits2.common.items.TypedItem;
import nl.dgoossens.chiselsandbits2.common.network.client.COpenBitBagPacket;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.items.TapeMeasureItem;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * All client side methods are split into two classes:
 *    - events are in ClientSide
 *    - everything else is in here
 */
public class ClientSideHelper {
    //--- GENERAL ---
    //Managers
    private final RenderingManager RENDER_MANAGER;

    //General Constants
    protected static final double BIT_SIZE = 1.0 / 16.0;
    protected static final double HALF_BIT = BIT_SIZE / 2.0f;

    //Resource Locations for Icons
    protected static final HashMap<IItemMode, ResourceLocation> modeIconLocations = new HashMap<>();
    protected static final HashMap<IMenuAction, ResourceLocation> menuActionLocations = new HashMap<>();

    //Tape Measure
    protected List<Measurement> tapeMeasurements = new ArrayList<>();
    protected BitLocation tapeMeasureCache;
    protected BitLocation selectionStart;
    private BitOperation operation;

    //Ghost Rendering
    protected IBakedModel ghostCache = null;
    protected BlockPos previousPosition;
    protected BlockPos previousPartial;
    protected ItemStack previousItem;
    protected IntegerBox modelBounds;
    protected BlockState previousState;
    protected boolean previousSilhoutte, previousOffGrid;
    protected IItemMode previousMode;
    protected long previousTileIteration = -Long.MAX_VALUE;

    public ClientSideHelper() {
        RENDER_MANAGER = new RenderingManager();
    }

    /**
     * Get the general rendering manager.
     */
    public RenderingManager getRenderingManager() {
        return RENDER_MANAGER;
    }

    /**
     * Cleans up some data when a player leaves the current save game.
     * To be more exact: this should be called whenever the previously assumed dimensions
     * are no longer the same. E.g. switching multiplayer servers.
     */
    public void clean() {
        tapeMeasurements.clear();
        tapeMeasureCache = null;
        selectionStart = null;
        operation = null;

        resetPlacementGhost();
        modelBounds = null;

        ChiselsAndBits2.getInstance().getUndoTracker().clean();
        RadialMenu.RADIAL_MENU.cleanup();
    }

    /**
     * Get the current bit operation.
     */
    protected BitOperation getOperation() {
        if(operation != null) return operation;
        //Morphing bit is mainly meant for placement so we show the placement highlight preferred over the removal highlight.
        if(Minecraft.getInstance().player.getHeldItemMainhand().getItem() instanceof MorphingBitItem) return BitOperation.PLACE;
        return BitOperation.REMOVE;
    }

    /**
     * Server friendly get player method to get the client
     * main player.
     */
    public PlayerEntity getPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * Get the resource location of the icon for the mode, will return null
     * if the mode has no icon.
     */
    public static ResourceLocation getModeIconLocation(final IItemMode mode) {
        return modeIconLocations.get(mode);
    }

    /**
     * Get the resource location of the icon for the menu action, will return null
     * if the action has no icon.
     */
    public static ResourceLocation getMenuActionIconLocation(final IMenuAction action) {
        return menuActionLocations.get(action);
    }

    /**
     * Returns true if this player inventory's hotbar has at least one item
     * which wants to render a toolbar icon.
     */
    public static boolean hasToolbarIconItem(PlayerInventory inventory) {
        for (int slot = 8; slot >= 0; --slot) {
            if (inventory.mainInventory.get(slot).getItem() instanceof IItemMenu && ((IItemMenu) inventory.mainInventory.get(slot).getItem()).showIconInHotbar())
                return true;
        }
        return false;
    }

    /**
     * Sets the starting location for a selection box. It's also important to specify the
     * operation as the bounding box is rendered different depending on if this is REMOVE or PLACE.
     */
    public void setSelectionStart(BitOperation operation, BitLocation bitLoc) {
        selectionStart = bitLoc;
        this.operation = operation;
    }

    /**
     * Returns if there is currently a selection started with this given operation.
     * A new selection should be started if another operation type is used regardless if
     * a selection was already started.
     */
    public boolean hasSelectionStart(BitOperation operation) {
        if(!operation.equals(this.operation)) return false;
        return selectionStart != null;
    }

    /**
     * Get the starting point of the current selection given the operation.
     */
    public BitLocation getSelectionStart(BitOperation operation) {
        if(!operation.equals(this.operation)) return null;
        return selectionStart;
    }

    /**
     * Resets the selection completely.
     */
    public void resetSelectionStart() {
        selectionStart = null;
        operation = null;
    }

    /**
     * Uses the tape measure with a given pre specified ray trace where the player is looking.
     * Automatically handles whether or not a selection has already started and caching the measurement.
     */
    public void useTapeMeasure(BlockRayTraceResult rayTrace) {
        //Clear measurements if there are measurements and we're not currently selecting one.
        if(tapeMeasureCache == null && Minecraft.getInstance().player.isCrouching() && !ChiselsAndBits2.getInstance().getClient().tapeMeasurements.isEmpty()) {
            ChiselsAndBits2.getInstance().getClient().tapeMeasurements.clear();
            Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent("general."+ChiselsAndBits2.MOD_ID+".info.cleared_measurements"), true);
            return;
        }

        final BitLocation location = new BitLocation(rayTrace, true, BitOperation.REMOVE);
        if(tapeMeasureCache == null) {
            //First Selection
            tapeMeasureCache = location;
        } else {
            //Second Selection
            final PlayerEntity player = Minecraft.getInstance().player;
            final ItemStack stack = player.getHeldItemMainhand();

            //Security measure.
            if(!(stack.getItem() instanceof TapeMeasureItem)) return;

            while(tapeMeasurements.size() >= ChiselsAndBits2.getInstance().getConfig().tapeMeasureLimit.get()) {
                tapeMeasurements.remove(0); //Remove the oldest one.
            }
            tapeMeasurements.add(new Measurement(tapeMeasureCache, location, ((TapeMeasureItem) stack.getItem()).getColour(stack), ((TapeMeasureItem) stack.getItem()).getSelectedMode(stack), player.dimension));
            tapeMeasureCache = null;
        }
    }

    /**
     * Renders the tape measure boxes.
     */
    public void renderTapeMeasureBoxes(float partialTicks) {
        final PlayerEntity player = Minecraft.getInstance().player;
        for(Measurement box : tapeMeasurements) {
            if(!player.dimension.equals(box.dimension)) continue;
            renderSelectionBox(true, player, box.first, box.second, partialTicks, BitOperation.REMOVE, new Color(box.colour.getColour()), box.mode);
        }
    }

    /**
     * Draws the highlights around blocks. Called from RenderWorldLastEvent to render on top of blocks as the normal event renders partially behind them.
     */
    public boolean drawBlockHighlight(float partialTicks) {
        if(Minecraft.getInstance().objectMouseOver.getType() == RayTraceResult.Type.BLOCK) {
            final PlayerEntity player = Minecraft.getInstance().player;
            final ItemStack stack = player.getHeldItemMainhand();
            //As this is rendering code and it gets called many times per tick, I try to minimise local variables.
            boolean tapeMeasure = stack.getItem() instanceof TapeMeasureItem;
            boolean renderHighlight = tapeMeasure;
            if(!renderHighlight && stack.getItem() instanceof IBitModifyItem) {
                IBitModifyItem i = (IBitModifyItem) stack.getItem();
                renderHighlight = i.canPerformModification(IBitModifyItem.ModificationType.BUILD) || i.canPerformModification(IBitModifyItem.ModificationType.EXTRACT);
            }
            if (renderHighlight && stack.getItem() instanceof TypedItem) { //has to be a typed item
                final RayTraceResult rayTrace = ChiselUtil.rayTrace(player);
                if (rayTrace == null || rayTrace.getType() != RayTraceResult.Type.BLOCK)
                    return false;

                final World world = Minecraft.getInstance().world;
                final BitLocation location = new BitLocation((BlockRayTraceResult) rayTrace, true, BitOperation.REMOVE); //We always show the removal box, never the placement one.
                final TileEntity data = world.getTileEntity(location.blockPos);

                //We only show this box if this block is chiselable and this block at this position is chiselable.
                if (!tapeMeasure && !ChiselsAndBits2.getInstance().getAPI().getRestrictions().canChiselBlock(world.getBlockState(location.blockPos))) return false;
                //The highlight not showing up when you can't chisel in a specific block isn't worth all of the code that needs to be checked for it.
                //if(!ChiselUtil.canChiselPosition(location.getBlockPos(), player, state, ((BlockRayTraceResult) mop).getFace())) return;

                //Rendering drawn region bounding box
                final BitLocation other = tapeMeasure ? ChiselsAndBits2.getInstance().getClient().tapeMeasureCache : ChiselsAndBits2.getInstance().getClient().selectionStart;
                final BitOperation operation = tapeMeasure ? BitOperation.REMOVE : ChiselsAndBits2.getInstance().getClient().getOperation();
                if ((tapeMeasure || ItemPropertyUtil.isItemMode(stack, ItemMode.CHISEL_DRAWN_REGION)) && other != null) {
                    ChiselsAndBits2.getInstance().getClient().renderSelectionBox(tapeMeasure, player, location, other, partialTicks, operation, tapeMeasure ? new Color(((TapeMeasureItem) stack.getItem()).getColour(stack).getColour()) : null, ((TypedItem) stack.getItem()).getSelectedMode(stack));
                    return true;
                }
                //Tape measure never displays the small cube.
                if(tapeMeasure) return false;

                //This method call is super complicated, but it saves having way more local variables than necessary.
                // (although I don't know if limiting local variables actually matters)
                RenderingAssistant.drawSelectionBoundingBoxIfExists(
                        ChiselTypeIterator.create(
                                VoxelBlob.DIMENSION, location.bitX, location.bitY, location.bitZ,
                                new VoxelRegionSrc(player, world, location.blockPos, 1),
                                ((TypedItem) stack.getItem()).getSelectedMode(stack),
                                ((BlockRayTraceResult) rayTrace).getFace(),
                                operation.equals(BitOperation.PLACE)
                        ).getBoundingBox(
                                !(data instanceof ChiseledBlockTileEntity) ? (new VoxelBlob().fill(BitUtil.getBlockId(world.getBlockState(location.blockPos))))
                                        : ((ChiseledBlockTileEntity) data).getVoxelBlob(), true
                        ),
                        location.blockPos, player, partialTicks, false, 0, 0, 0, 102, 32);
                return true;
            }
        }
        return false;
    }

    /**
     * Renders the selection boxes as used by the tape measure and drawn region mode.
     */
    public void renderSelectionBox(boolean tapeMeasure, PlayerEntity player, BitLocation location, BitLocation other, float partialTicks, BitOperation operation, @Nullable Color c, @Nullable IItemMode mode) {
        AxisAlignedBB bb = null;

        //Don't do these calculations if we don't have to.
        if (!tapeMeasure || !mode.equals(ItemMode.TAPEMEASURE_DISTANCE)) {
            ChiselIterator oneEnd, otherEnd;
            if(mode.equals(ItemMode.TAPEMEASURE_BLOCK)) {
                boolean x = location.blockPos.getX() > other.blockPos.getX(), y = location.blockPos.getY() > other.blockPos.getY(), z = location.blockPos.getZ() > other.blockPos.getZ();
                oneEnd = ChiselTypeIterator.create(VoxelBlob.DIMENSION, x ? 15 : 0, y ? 15 : 0, z ? 15 : 0, VoxelBlob.NULL_BLOB, ItemMode.CHISEL_SINGLE, Direction.UP, operation.equals(BitOperation.PLACE));
                otherEnd = ChiselTypeIterator.create(VoxelBlob.DIMENSION, !x ? 15 : 0, !y ? 15 : 0, !z ? 15 : 0, VoxelBlob.NULL_BLOB, ItemMode.CHISEL_SINGLE, Direction.UP, operation.equals(BitOperation.PLACE));
            } else {
                oneEnd = ChiselTypeIterator.create(VoxelBlob.DIMENSION, location.bitX, location.bitY, location.bitZ, VoxelBlob.NULL_BLOB, ItemMode.CHISEL_SINGLE, Direction.UP, operation.equals(BitOperation.PLACE));
                otherEnd = ChiselTypeIterator.create(VoxelBlob.DIMENSION, other.bitX, other.bitY, other.bitZ, VoxelBlob.NULL_BLOB, ItemMode.CHISEL_SINGLE, Direction.UP, operation.equals(BitOperation.PLACE));
            }

            final AxisAlignedBB a = oneEnd.getBoundingBox(VoxelBlob.NULL_BLOB, false).offset(location.blockPos.getX(), location.blockPos.getY(), location.blockPos.getZ());
            final AxisAlignedBB b = otherEnd.getBoundingBox(VoxelBlob.NULL_BLOB, false).offset(other.blockPos.getX(), other.blockPos.getY(), other.blockPos.getZ());

            bb = a.union(b);
        }

        if(tapeMeasure) {
            //Draw length indicator
            final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
            final double x = renderInfo.getProjectedView().x;
            final double y = renderInfo.getProjectedView().y;
            final double z = renderInfo.getProjectedView().z;

            if(mode.equals(ItemMode.TAPEMEASURE_DISTANCE)) {
                final Vec3d a = buildTapeMeasureDistanceVector(location);
                final Vec3d b = buildTapeMeasureDistanceVector(other);

                RenderingAssistant.drawLineWithColor(a, b, BlockPos.ZERO, player, partialTicks, false, c.getRed(), c.getGreen(), c.getBlue(), 102, 32);

                GlStateManager.disableDepthTest();
                GlStateManager.disableCull();

                final double length = a.distanceTo(b) + BIT_SIZE;
                renderTapeMeasureLabel(partialTicks, (a.getX() + b.getX()) * 0.5 - x, (a.getY() + b.getY()) * 0.5 - y, (a.getZ() + b.getZ()) * 0.5 - z, length, c.getRed(), c.getGreen(), c.getBlue());

                GlStateManager.enableDepthTest();
                GlStateManager.enableCull();
            } else {
                RenderingAssistant.drawSelectionBoundingBoxIfExists(bb, BlockPos.ZERO, player, partialTicks, false, c.getRed(), c.getGreen(), c.getBlue(), 102, 32);

                GlStateManager.disableDepthTest();
                GlStateManager.disableCull();

                final double lengthX = bb.maxX - bb.minX;
                final double lengthY = bb.maxY - bb.minY;
                final double lengthZ = bb.maxZ - bb.minZ;
                renderTapeMeasureLabel(partialTicks, bb.minX - x, (bb.maxY + bb.minY) * 0.5 - y, bb.minZ - z, lengthY, c.getRed(), c.getGreen(), c.getBlue());
                renderTapeMeasureLabel(partialTicks, (bb.minX + bb.maxX) * 0.5 - x, bb.minY - y, bb.minZ - z, lengthX, c.getRed(), c.getGreen(), c.getBlue());
                renderTapeMeasureLabel(partialTicks, bb.minX - x, bb.minY - y, (bb.minZ + bb.maxZ) * 0.5 - z, lengthZ, c.getRed(), c.getGreen(), c.getBlue());

                GlStateManager.enableDepthTest();
                GlStateManager.enableCull();
            }
        } else {
            final double maxSize = ChiselsAndBits2.getInstance().getConfig().maxDrawnRegionSize.get() + 0.001;
            if (bb.maxX - bb.minX <= maxSize && bb.maxY - bb.minY <= maxSize && bb.maxZ - bb.minZ <= maxSize) {
                RenderingAssistant.drawSelectionBoundingBoxIfExists(bb, BlockPos.ZERO, player, partialTicks, false, 0, 0, 0, 102, 32);
            }
        }
    }

    /**
     * Renders the label next to the tape measure bounding box.
     */
    protected void renderTapeMeasureLabel(final float partialTicks, final double x, final double y, final double z, final double len, final int red, final int green, final int blue) {
        final double letterSize = 5.0;
        final double zScale = 0.001;

        final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        final String size = formatTapeMeasureLabel(len);

        GlStateManager.pushMatrix();
        GlStateManager.translated(x, y + getScale(len) * letterSize, z);
        final Entity view = Minecraft.getInstance().getRenderViewEntity();
        if (view != null) {
            final float yaw = view.prevRotationYaw + (view.rotationYaw - view.prevRotationYaw) * partialTicks;
            GlStateManager.rotatef(180 + -yaw, 0f, 1f, 0f);

            final float pitch = view.prevRotationPitch + (view.rotationPitch - view.prevRotationPitch) * partialTicks;
            GlStateManager.rotatef(-pitch, 1f, 0f, 0f);
        }
        GlStateManager.scaled(getScale(len), -getScale(len), zScale);
        GlStateManager.translated(-fontRenderer.getStringWidth(size) * 0.5, 0, 0);
        fontRenderer.drawStringWithShadow(size, 0, 0, red << 16 | green << 8 | blue);
        GlStateManager.popMatrix();
    }

    /**
     * Get the scale of the tape measure label based on the length of the measured area.
     */
    protected double getScale(final double maxLen) {
        final double maxFontSize = 0.04;
        final double minFontSize = 0.004;

        final double delta = Math.min(1.0, maxLen / 4.0);
        double scale = maxFontSize * delta + minFontSize * (1.0 - delta);
        if (maxLen < 0.25)
            scale = minFontSize;

        return Math.min(maxFontSize, scale);
    }

    /**
     * Format the label of the tape measure into the proper format.
     */
    protected String formatTapeMeasureLabel(final double d) {
        final double blocks = Math.floor(d);
        final double bits = d - blocks;

        final StringBuilder b = new StringBuilder();

        if (blocks > 0)
            b.append((int) blocks).append("m");

        if (bits * 16 > 0.9999) {
            if (b.length() > 0)
                b.append(" ");
            b.append((int) (bits * 16)).append("b");
        }

        return b.toString();
    }

    /**
     * Builds the vector pointing to a bit location's bit.
     */
    protected Vec3d buildTapeMeasureDistanceVector(BitLocation a) {
        final double ax = a.blockPos.getX() + BIT_SIZE * a.bitX + HALF_BIT;
        final double ay = a.blockPos.getY() + BIT_SIZE * a.bitY + HALF_BIT;
        final double az = a.blockPos.getZ() + BIT_SIZE * a.bitZ + HALF_BIT;
        return new Vec3d(ax, ay, az);
    }

    /**
     * Renders the placement ghost when holding a chiseled block or pattern.
     */
    public void renderPlacementGhost(float partialTicks) {
        //If placement ghosts are disabled, don't render anything.
        if (!ChiselsAndBits2.getInstance().getConfig().enablePlacementGhost.get()) return;
        final PlayerEntity player = Minecraft.getInstance().player;
        final ItemStack currentItem = player.getHeldItemMainhand();

        if(!currentItem.isEmpty() && currentItem.getItem() instanceof ChiseledBlockItem) {
            if (!currentItem.hasTag()) return;

            RayTraceResult rtr = ChiselUtil.rayTrace(player);
            if(!(rtr instanceof BlockRayTraceResult) || rtr.getType() != RayTraceResult.Type.BLOCK) return;
            final BlockRayTraceResult r = (BlockRayTraceResult) rtr;
            final ItemMode mode = ClientItemPropertyUtil.getGlobalCBM();

            BlockPos offset = r.getPos();
            Direction face = r.getFace();

            final NBTBlobConverter nbt = new NBTBlobConverter();
            nbt.readChiselData(currentItem.getChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());

            if (player.isCrouching() && !ClientItemPropertyUtil.getGlobalCBM().equals(ItemMode.CHISELED_BLOCK_GRID)) {
                final BitLocation bl = new BitLocation(r, true, BitOperation.PLACE);
                //We don't make this darker if we can't place here because the calculations are far too expensive to do every time.
                ChiselsAndBits2.getInstance().getClient().showGhost(currentItem, nbt, player.world, bl.blockPos, face, new BlockPos(bl.bitX, bl.bitY, bl.bitZ), partialTicks, true, () -> !BlockPlacementLogic.isPlaceableOffgrid(player, player.world, face, bl, currentItem));
            } else {
                //If we can already place where we're looking we don't have to move.
                //On grid we don't do this.
                if((!ChiselUtil.isBlockReplaceable(player.world, offset, player, face, false) && ClientItemPropertyUtil.getGlobalCBM() == ItemMode.CHISELED_BLOCK_GRID) || (!(player.world.getTileEntity(offset) instanceof ChiseledBlockTileEntity) && !BlockPlacementLogic.isNormallyPlaceable(player, player.world, offset, face, nbt, mode)))
                    offset = offset.offset(face);

                final BlockPos finalOffset = offset;
                ChiselsAndBits2.getInstance().getClient().showGhost(currentItem, nbt, player.world, offset, face, BlockPos.ZERO, partialTicks, false, () -> !BlockPlacementLogic.isNormallyPlaceable(player, player.world, finalOffset, face, nbt, mode));
            }
        }
    }

    /**
     * Forces the placement ghost to get re-rendered, called when the chiseled block item mode is changed.
     */
    public void resetPlacementGhost() {
        ghostCache = null;
        previousPartial = null;
        previousPosition = null;
        previousItem = null;
        previousState = null;
        previousTileIteration = -Long.MAX_VALUE;
        previousSilhoutte = false;
        previousOffGrid = false;
        previousMode = null;
    }

    /**
     * Determines if there is a difference between te and previousTile.
     */
    private boolean didTileChange(final TileEntity te) {
        if(te == null && previousTileIteration == -Long.MAX_VALUE) return false; //Both null? Same.
        if(te == null || previousTileIteration == -Long.MAX_VALUE) return true; //Not both not null? Different!
        if(te instanceof ChiseledBlockTileEntity) {
            final ChiseledBlockTileEntity newTile = (ChiseledBlockTileEntity) te;
            return newTile.getIteration() != previousTileIteration;
        }
        return true; //It changed if it isn't a chiseled block anymore.
    }

    /**
     * Shows the ghost of the chiseled block in item at the position offset by the partial in bits.
     */
    protected void showGhost(ItemStack item, NBTBlobConverter c, World world, BlockPos pos, Direction face, BlockPos partial, float partialTicks, boolean offGrid, final Supplier<Boolean> silhoutte) {
        final PlayerEntity player = Minecraft.getInstance().player;
        IBakedModel model = null;
        if(ghostCache != null && ClientItemPropertyUtil.getGlobalCBM().equals(previousMode) && item.equals(previousItem) && pos.equals(previousPosition) && partial.equals(previousPartial) && offGrid == previousOffGrid && world.getBlockState(pos).equals(previousState) && !didTileChange(world.getTileEntity(pos)))
            model = ghostCache;
        else {
            previousPosition = pos;
            previousPartial = partial;
            previousItem = item;
            previousSilhoutte = silhoutte.get();
            previousState = world.getBlockState(pos);
            previousMode = ClientItemPropertyUtil.getGlobalCBM();
            previousOffGrid = offGrid;

            final TileEntity te = world.getTileEntity(pos);

            boolean modified = false;
            VoxelBlob blob = c.getVoxelBlob();
            if(te instanceof ChiseledBlockTileEntity) {
                previousTileIteration = ((ChiseledBlockTileEntity) te).getIteration();
                VoxelBlob b = ((ChiseledBlockTileEntity) te).getVoxelBlob();
                if(ClientItemPropertyUtil.getGlobalCBM().equals(ItemMode.CHISELED_BLOCK_MERGE)) {
                    blob.intersect(b);
                    modified = true;
                }
            } else previousTileIteration = -Long.MAX_VALUE;
            modelBounds = blob.getBounds();

            if(modified) {
                c.setBlob(blob);
                item = c.getItemStack();
            }

            model = Minecraft.getInstance().getItemRenderer().getItemModelWithOverrides(item, player.getEntityWorld(), player);
            ghostCache = model;
        }

        final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        final double x = renderInfo.getProjectedView().x;
        final double y = renderInfo.getProjectedView().y;
        final double z = renderInfo.getProjectedView().z;

        GlStateManager.pushMatrix();
        GlStateManager.translated(pos.getX() - x, pos.getY() - y, pos.getZ() - z);
        if (!partial.equals(BlockPos.ZERO)) {
            final BlockPos t = BlockPlacementLogic.getPartialOffset(face, partial, modelBounds);
            final double fullScale = 1.0 / VoxelBlob.DIMENSION;
            GlStateManager.translated(t.getX() * fullScale, t.getY() * fullScale, t.getZ() * fullScale);
        }

        //Always expand the offgrid, silhouttes and otherwise if overlap or fit. We expand offgrids as otherwise the calculations are too intensive. (expanse isn't really noticable anyways)
        RenderingAssistant.renderGhostModel(model, player.world, partialTicks, pos, previousSilhoutte, previousOffGrid || previousSilhoutte || ClientItemPropertyUtil.getGlobalCBM() == ItemMode.CHISELED_BLOCK_OVERLAP || ClientItemPropertyUtil.getGlobalCBM() == ItemMode.CHISELED_BLOCK_FIT);
        GlStateManager.popMatrix();
    }

    //--- SUB CLASSES ---
    /**
     * Represents a box drawn by the tape measure.
     */
    public static class Measurement {
        private BitLocation first, second;
        private DimensionType dimension;
        private DyedItemColour colour;
        private IItemMode mode;

        public Measurement(BitLocation first, BitLocation second, DyedItemColour colour, IItemMode mode, DimensionType dimension) {
            this.first = first;
            this.second = second;
            this.colour = colour;
            this.mode = mode;
            this.dimension = dimension;
        }
    }

    //--- BIT BAG ---
    public void openBitBag() {
        ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new COpenBitBagPacket());
    }
}
