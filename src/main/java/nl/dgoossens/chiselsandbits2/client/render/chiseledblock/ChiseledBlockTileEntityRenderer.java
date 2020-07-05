package nl.dgoossens.chiselsandbits2.client.render.chiseledblock;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

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
        int primaryBlock = tileEntity.getVoxelState().getPrimaryBlock();
        if(primaryBlock == -1) primaryBlock = BitUtil.getBlockId(ChiselsAndBits2.getInstance().getRegister().PREVIEW_BLOCK.get().getDefaultState());
        IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(BitUtil.getBlockState(primaryBlock));
        ResourceLocation texture = model.getParticleTexture().getName();

        ResourceLocation atlas = PlayerContainer.LOCATION_BLOCKS_TEXTURE;
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(atlas).apply(texture);
        IVertexBuilder vertexBuilder = sprite.wrapBuffer(buffer.getBuffer(RenderType.getEntityCutout(atlas)));

        //TEMP Render a textureless rectangle within the selection bounding box
        ModelRenderer mr = new ModelRenderer(64, 64, 0, 0);
        VoxelShape selectionBox = tileEntity.getVoxelState().getSelectionShape();
        mr.addBox((float) selectionBox.getStart(Direction.Axis.X) * 16, (float) selectionBox.getStart(Direction.Axis.Y) * 16, (float) selectionBox.getStart(Direction.Axis.Z) * 16,
                (float) selectionBox.getEnd(Direction.Axis.X) * 16, (float) selectionBox.getEnd(Direction.Axis.Y) * 16, (float) selectionBox.getEnd(Direction.Axis.Z) * 16);
        mr.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
    }
}
