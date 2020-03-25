package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.BlockPlacementLogic;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.NBTBlobConverter;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;

/**
 * The class responsible for rendering the ghost model of the held chiseled
 * block.
 */
public class GhostModelRenderer {
    //All these values are the states of the currently shown ghost. If they change we need to regenerate the model.
    private IBakedModel model = null;
    private BitLocation location;
    private int heldSlot;
    private IntegerBox modelBounds;
    private BlockState state;
    private PlayerItemMode mode;
    private Direction face;
    private boolean isEmpty, silhouette, offGrid;
    private long previousTileIteration = -Long.MAX_VALUE;;

    public GhostModelRenderer(ItemStack item, PlayerEntity player, BitLocation location, Direction face, boolean offGrid) {
        if(item.isEmpty()) {
            isEmpty = true;
            return;
        }

        final NBTBlobConverter c = new NBTBlobConverter();
        c.readChiselData(item.getChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());

        this.heldSlot = player.inventory.currentItem;
        this.location = location;
        this.mode = ClientItemPropertyUtil.getChiseledBlockMode();

        //Whether or not this is a silhoutte depends on whether or not it is placeable, which depends on if this is off-grid or not.
        if(offGrid) {
            this.silhouette = BlockPlacementLogic.isNotPlaceableOffGrid(player, player.world, face, location, item);
        } else {
            //We have a supplier for the NBTBlobConverter as we have it ready for use here so there is no need to recalculate it.
            this.silhouette = BlockPlacementLogic.isNotPlaceable(player, player.world, location.blockPos, face, mode, () -> c);
        }
        this.state = player.world.getBlockState(location.blockPos);
        this.face = face;
        this.offGrid = offGrid;

        final TileEntity te = player.world.getTileEntity(location.blockPos);

        boolean modified = false;
        VoxelBlob blob = c.getVoxelBlob();
        if(te instanceof ChiseledBlockTileEntity) {
            previousTileIteration = ((ChiseledBlockTileEntity) te).getIteration();
            VoxelBlob b = ((ChiseledBlockTileEntity) te).getVoxelBlob();
            if(ClientItemPropertyUtil.getChiseledBlockMode().equals(PlayerItemMode.CHISELED_BLOCK_MERGE)) {
                blob.intersect(b);
                modified = true;
            }
        }
        modelBounds = blob.getBounds();

        //If we modified the blob we have to reapply it and build a new item.
        if(modified) {
            c.setBlob(blob);
            item = c.getItemStack();
        }

        model = Minecraft.getInstance().getItemRenderer().getItemModelWithOverrides(item, player.getEntityWorld(), player);
    }

    public boolean isSilhouette() {
        return silhouette;
    }

    public boolean shouldExpand() {
        PlayerItemMode cbm = ClientItemPropertyUtil.getChiseledBlockMode();
        return offGrid || silhouette || cbm == PlayerItemMode.CHISELED_BLOCK_OVERLAP || cbm == PlayerItemMode.CHISELED_BLOCK_FIT;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public IBakedModel getModel() {
        return model;
    }

    /**
     * Returns whether or not this model is still valid if we have new inputs.
     */
    public boolean isValid(PlayerEntity player, BitLocation pos, Direction face, boolean offGrid) {
        if(isEmpty) return false;
        if (model != null && heldSlot == player.inventory.currentItem && ClientItemPropertyUtil.getChiseledBlockMode().equals(mode) && this.location.equals(pos) && offGrid == this.offGrid && face == this.face && player.world.getBlockState(pos.blockPos).equals(state) && !didTileChange(player.world.getTileEntity(pos.blockPos)))
            return true;
        else
            return false;
    }

    /**
     * Determines if there is a difference between te and previousTile.
     */
    public boolean didTileChange(final TileEntity te) {
        if(te == null && previousTileIteration == -Long.MAX_VALUE) return false; //Both null? Same.
        if(te == null || previousTileIteration == -Long.MAX_VALUE) return true; //Not both not null? Different!
        if(te instanceof ChiseledBlockTileEntity) {
            final ChiseledBlockTileEntity newTile = (ChiseledBlockTileEntity) te;
            return newTile.getIteration() != previousTileIteration;
        }
        return true; //It changed if it isn't a chiseled block anymore.
    }

    /**
     * Renders this ghost model.
     */
    //TODO modernise this rendering code
    public void render(float partialTicks) {
        if(isEmpty) return;
        final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        final double x = renderInfo.getProjectedView().x;
        final double y = renderInfo.getProjectedView().y;
        final double z = renderInfo.getProjectedView().z;

        PlayerEntity player = Minecraft.getInstance().player;
        GlStateManager.pushMatrix();
        BlockPos position = location.blockPos;
        GlStateManager.translated(position.getX() - x, position.getY() - y, position.getZ() - z);
        if (!location.equals(BlockPos.ZERO)) {
            final BlockPos t = BlockPlacementLogic.getPartialOffset(face, new BlockPos(location.bitX, location.bitY, location.bitZ), modelBounds);
            final double fullScale = 1.0 / VoxelBlob.DIMENSION;
            GlStateManager.translated(t.getX() * fullScale, t.getY() * fullScale, t.getZ() * fullScale);
        }

        //Always expand the offgrid, silhouettes and otherwise if overlap or fit. We always expand offgrid ghosts as otherwise the calculations are too intensive. (the expanding isn't really noticeable anyways)
        //TODO RenderingAssistant.renderGhostModel(model, player.world, partialTicks, position, isSilhouette(), shouldExpand());
        GlStateManager.popMatrix();
    }
}
