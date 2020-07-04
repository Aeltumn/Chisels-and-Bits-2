package nl.dgoossens.chiselsandbits2.client.render.chiseledblock;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.DualBrightnessCallback;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntityMerger;
import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;

public class ChiseledBlockTileEntityRenderer extends TileEntityRenderer<ChiseledBlockTileEntity> {
    private final ModelRenderer singleLid;
    private final ModelRenderer singleBottom;

    public ChiseledBlockTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);

        this.singleBottom = new ModelRenderer(64, 64, 0, 19);
        this.singleBottom.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, 0.0F);
        this.singleLid = new ModelRenderer(64, 64, 0, 0);
        this.singleLid.addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, 0.0F);
        this.singleLid.rotationPointY = 9.0F;
        this.singleLid.rotationPointZ = 1.0F;
    }

    @Override
    public void render(ChiseledBlockTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.push();
        float f = Direction.NORTH.getHorizontalAngle();
        matrixStackIn.translate(0.5D, 0.5D, 0.5D);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(-f));
        matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
        TileEntityMerger.ICallbackWrapper<? extends ChiseledBlockTileEntity> icallbackwrapper = TileEntityMerger.ICallback::func_225537_b_;

        float f1 = 0.0f;
        f1 = 1.0F - f1;
        f1 = 1.0F - f1 * f1 * f1;
        int i = icallbackwrapper.apply(new DualBrightnessCallback<>()).applyAsInt(combinedLightIn);
        Material material = Atlases.CHEST_MATERIAL;
        IVertexBuilder ivertexbuilder = material.getBuffer(bufferIn, RenderType::getEntityCutout);
        this.renderModels(matrixStackIn, ivertexbuilder, f1, i, combinedOverlayIn);
        matrixStackIn.pop();
    }

    private void renderModels(MatrixStack matrixStackIn, IVertexBuilder bufferIn, float lidAngle, int combinedLightIn, int combinedOverlayIn) {
        this.singleLid.rotateAngleX = -(lidAngle * ((float)Math.PI / 2F));
        this.singleLid.render(matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
        this.singleBottom.render(matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
    }
}
