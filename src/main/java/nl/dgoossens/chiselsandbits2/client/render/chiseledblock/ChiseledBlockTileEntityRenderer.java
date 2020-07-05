package nl.dgoossens.chiselsandbits2.client.render.chiseledblock;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.DualBrightnessCallback;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntityMerger;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;

public class ChiseledBlockTileEntityRenderer extends TileEntityRenderer<ChiseledBlockTileEntity> {
    private final ModelRenderer singleBottom;

    public ChiseledBlockTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);

        this.singleBottom = new ModelRenderer(64, 64, 0, 19);
        this.singleBottom.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, 0.0F);
    }

    @Override
    public void render(ChiseledBlockTileEntity tileEntity, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        // Rotate the matrix stack to face the proper direction
        float f = Direction.NORTH.getHorizontalAngle();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        matrixStack.rotate(Vector3f.YP.rotationDegrees(-f));
        matrixStack.translate(-0.5D, -0.5D, -0.5D);

        // Get the vertex buffer to draw into and draw
        IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.getEntityCutout(new ResourceLocation("minecraft:missingno")));
        this.singleBottom.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
    }
}
