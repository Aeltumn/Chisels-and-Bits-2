package nl.dgoossens.chiselsandbits2.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.api.item.ItemMode;
import nl.dgoossens.chiselsandbits2.api.bit.BitOperation;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelTile;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.api.iterator.ChiselIterator;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;

/**
 * The class responsible for rendering the selection box.
 * Because of the more complicated shapes of the bounding boxes
 * these are cached and only updated whenever we move.
 */
public class SelectionBoxRenderer extends CachedRenderedObject {
    private AxisAlignedBB selectionBoundingBox;

    public SelectionBoxRenderer(ItemStack item, PlayerEntity player, BitLocation location, Direction face, BitOperation operation, ItemMode mode) {
        super(item, player, location, face, mode);
        if (isEmpty())
            return;

        final TileEntity data = player.world.getTileEntity(location.blockPos);
        final ChiselIterator iterator = getMode().getIterator(new BlockPos(location.bitX, location.bitY, location.bitZ), face, operation, data == null ? null : (VoxelTile) data);
        final VoxelBlob blob = data instanceof ChiseledBlockTileEntity ? ((ChiseledBlockTileEntity) data).getVoxelBlob() : VoxelBlob.FULL_BLOB;
        selectionBoundingBox = iterator.getBoundingBox(blob).orElse(null);

        if (selectionBoundingBox == null)
            invalidate();
    }

    @Override
    public void render(MatrixStack matrix, IRenderTypeBuffer buffer, float partialTicks) {
        if (isEmpty()) return;
        RenderingAssistant.drawBoundingBox(matrix, buffer, selectionBoundingBox, getLocation().blockPos);
    }
}
