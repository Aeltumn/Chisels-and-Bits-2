package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
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
public class GhostModelRenderer extends CachedRenderedObject {
    //All these values are the states of the currently shown ghost. If they change we need to regenerate the model.
    private IBakedModel model;
    private IntegerBox modelBounds;
    private boolean silhouette, offGrid;

    public GhostModelRenderer(ItemStack item, PlayerEntity player, BitLocation location, Direction face, boolean offGrid) {
        super(item, player, location, face, ClientItemPropertyUtil.getChiseledBlockMode());
        if(isEmpty())
            return;

        final NBTBlobConverter c = new NBTBlobConverter();
        c.readChiselData(item.getChildTag(ChiselUtil.NBT_BLOCKENTITYTAG), VoxelVersions.getDefault());

        //Whether or not this is a silhoutte depends on whether or not it is placeable, which depends on if this is off-grid or not.
        this.offGrid = offGrid;
        if(offGrid) {
            this.silhouette = BlockPlacementLogic.isNotPlaceableOffGrid(player, player.world, face, location, item);
        } else {
            //We have a supplier for the NBTBlobConverter as we have it ready for use here so there is no need to recalculate it.
            this.silhouette = BlockPlacementLogic.isNotPlaceable(player, player.world, location.blockPos, face, (PlayerItemMode) getMode(), () -> c);
        }

        final TileEntity te = player.world.getTileEntity(location.blockPos);
        boolean modified = false;
        VoxelBlob blob = c.getVoxelBlob();
        if(te instanceof ChiseledBlockTileEntity) {
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
        //Always expand the offgrid, silhouettes and otherwise if overlap or fit. We always expand offgrid ghosts as otherwise the calculations are too intensive. (the expanding isn't really noticeable anyways)
        return offGrid || silhouette || cbm == PlayerItemMode.CHISELED_BLOCK_OVERLAP || cbm == PlayerItemMode.CHISELED_BLOCK_FIT;
    }

    @Override
    public void render(MatrixStack matrix, IRenderTypeBuffer buffer, float partialTicks) {
        if(isEmpty()) return;
        final ActiveRenderInfo renderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        final double x = renderInfo.getProjectedView().x;
        final double y = renderInfo.getProjectedView().y;
        final double z = renderInfo.getProjectedView().z;

        matrix.push();
        BitLocation location = getLocation();
        BlockPos position = location.blockPos;
        matrix.translate(position.getX() - x, position.getY() - y, position.getZ() - z);
        if (!location.equals(BlockPos.ZERO)) {
            final BlockPos t = BlockPlacementLogic.getPartialOffset(getFace(), new BlockPos(location.bitX, location.bitY, location.bitZ), modelBounds);
            final double fullScale = 1.0 / VoxelBlob.DIMENSION;
            matrix.translate(t.getX() * fullScale, t.getY() * fullScale, t.getZ() * fullScale);
        }

        //TODO Update RenderAssistant#renderGhostModel
        //RenderingAssistant.renderGhostModel(model, player.world, partialTicks, position, isSilhouette(), shouldExpand());
        matrix.pop();
    }
}
