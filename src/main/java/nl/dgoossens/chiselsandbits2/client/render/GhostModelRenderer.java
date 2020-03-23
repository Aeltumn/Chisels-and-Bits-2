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
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.BlockPlacementLogic;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.NBTBlobConverter;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.IntegerBox;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;

import java.util.function.Supplier;

/**
 * The class responsible for rendering the ghost model of the held chiseled
 * block.
 */
public class GhostModelRenderer {
    //All these values are the states of the currently shown ghost. If they change we need to regenerate the model.
    private IBakedModel model = null;
    private BlockPos position, partial; //partial is a blockpos with bitX, bitY and bitZ, only used by offgrid
    private int heldSlot;
    private IntegerBox modelBounds;
    private BlockState state;
    private IItemMode mode;
    private Direction face;
    private boolean isEmpty, silhouette, offGrid;
    private long previousTileIteration = -Long.MAX_VALUE;;

    public GhostModelRenderer(ItemStack item, NBTBlobConverter c, PlayerEntity player, BlockPos pos, BlockPos partial, Direction face, boolean offGrid, final Supplier<Boolean> silhouette) {
        if(item.isEmpty()) {
            isEmpty = true;
            return;
        }

        this.heldSlot = player.inventory.currentItem;
        this.position = pos;
        this.partial = partial;
        this.silhouette = silhouette.get();
        this.state = player.world.getBlockState(pos);
        this.mode = ClientItemPropertyUtil.getChiseledBlockMode();
        this.face = face;
        this.offGrid = offGrid;

        final TileEntity te = player.world.getTileEntity(pos);
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
    public boolean isValid(PlayerEntity player, BlockPos pos, BlockPos partial, Direction face, boolean offGrid) {
        if(isEmpty) return false;
        if (model != null && heldSlot == player.inventory.currentItem && ClientItemPropertyUtil.getChiseledBlockMode().equals(mode) && pos.equals(position) && offGrid == this.offGrid && partial.equals(this.partial) && face == this.face && player.world.getBlockState(pos).equals(state) && !didTileChange(player.world.getTileEntity(pos)))
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
        GlStateManager.translated(position.getX() - x, position.getY() - y, position.getZ() - z);
        if (!partial.equals(BlockPos.ZERO)) {
            final BlockPos t = BlockPlacementLogic.getPartialOffset(face, partial, modelBounds);
            final double fullScale = 1.0 / VoxelBlob.DIMENSION;
            GlStateManager.translated(t.getX() * fullScale, t.getY() * fullScale, t.getZ() * fullScale);
        }

        //Always expand the offgrid, silhouettes and otherwise if overlap or fit. We always expand offgrid ghosts as otherwise the calculations are too intensive. (the expanding isn't really noticeable anyways)
        //TODO RenderingAssistant.renderGhostModel(model, player.world, partialTicks, position, isSilhouette(), shouldExpand());
        GlStateManager.popMatrix();
    }
}
