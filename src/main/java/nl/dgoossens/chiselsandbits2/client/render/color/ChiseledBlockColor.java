package nl.dgoossens.chiselsandbits2.client.render.color;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import javax.annotation.Nullable;

public class ChiseledBlockColor extends ChiseledTintColor implements IBlockColor {
    private ILightReader world;
    private BlockPos pos;

    public ChiseledBlockColor() {}
    public ChiseledBlockColor(ILightReader world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public int getColor(BlockState state, @Nullable ILightReader world, @Nullable BlockPos pos, int tint) {
        if(world != null) this.world = world;
        if(pos != null) this.pos = pos;
        return getColor(tint);
    }

    @Override
    public int getDefaultColor(int t) {
        return Minecraft.getInstance().getBlockColors().getColor(BitUtil.getBlockState(t), world, pos, -1);
    }
}
