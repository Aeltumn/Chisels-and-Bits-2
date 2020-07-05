package nl.dgoossens.chiselsandbits2.client.render.chiseledblock;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;

public class ChiseledBlockTileEntityRenderer extends TileEntityRenderer<ChiseledBlockTileEntity> {
    public ChiseledBlockTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(ChiseledBlockTileEntity tileEntity, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        //Rotate the matrix stack to face the proper direction
        //float f = Direction.NORTH.getHorizontalAngle();
        //matrixStack.translate(0.5D, 0.5D, 0.5D);
        //matrixStack.rotate(Vector3f.YP.rotationDegrees(-f));
        //matrixStack.translate(-0.5D, -0.5D, -0.5D);

        //Get the vertex buffer to draw into and draw
        IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.getEntityCutout(new ResourceLocation("minecraft:missingno")));
        //TEMP Render a textureless rectangle within the selection bounding box
        ModelRenderer mr = new ModelRenderer(64, 64, 0, 19);
        VoxelShape selectionBox = tileEntity.getVoxelState().getSelectionShape();
        mr.addBox((float) selectionBox.getStart(Direction.Axis.X) * 16, (float) selectionBox.getStart(Direction.Axis.Y) * 16, (float) selectionBox.getStart(Direction.Axis.Z) * 16,
                (float) selectionBox.getEnd(Direction.Axis.X) * 16, (float) selectionBox.getEnd(Direction.Axis.Y) * 16, (float) selectionBox.getEnd(Direction.Axis.Z) * 16);
        mr.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
    }
}
