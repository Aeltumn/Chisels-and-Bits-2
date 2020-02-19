package nl.dgoossens.chiselsandbits2.client.render.chiseledblock.ter;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.MatrixApplyingVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Region;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockBaked;
import nl.dgoossens.chiselsandbits2.client.render.chiseledblock.model.ChiseledBlockSmartModel;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

public class BackgroundRenderer implements Callable<Tessellator> {
    private final Set<ChiseledBlockTileEntity> myPrivateList;
    private final Region cache;
    private final BlockPos chunkOffset;

    BackgroundRenderer(final Region cache, final BlockPos chunkOffset, final Collection<ChiseledBlockTileEntity> myList) {
        this.myPrivateList = new HashSet<>(myList);
        this.cache = cache;
        this.chunkOffset = chunkOffset;
    }

    @Override
    public Tessellator call() {
        Tessellator tessellator = null;
        try {
            tessellator = ChiselsAndBits2.getInstance().getClient().getRenderingManager().getRecycledTessellator();
            if (tessellator == null) {
                synchronized (Tessellator.class) {
                    tessellator = new Tessellator(2109952);
                }
            }
            final MatrixStack buffer = new MatrixStack();
            try {
                buffer.translate(-chunkOffset.getX(), -chunkOffset.getY(), -chunkOffset.getZ());
            } catch (final IllegalStateException e) {
                e.printStackTrace();
            }

            Random random = new Random();
            BlockRendererDispatcher blockRenderer = Minecraft.getInstance().getBlockRendererDispatcher();
            for (final ChiseledBlockTileEntity tx : myPrivateList) {
                if (!tx.isRemoved()) {
                    final ChiseledBlockBaked model = ChiselsAndBits2.getInstance().getClient().getRenderingManager().getCachedModel(tx);
                    //if (!model.isEmpty())
                        //blockRenderer.getBlockModelRenderer().renderModel(cache, model, tx.getBlockState(), tx.getPos(), buffer, new MatrixApplyingVertexBuilder());
                }
            }
            return tessellator;
        } catch(Exception x) {
            //Catch exceptions and print them here.
            x.printStackTrace();
            return tessellator;
        }
    }
}
