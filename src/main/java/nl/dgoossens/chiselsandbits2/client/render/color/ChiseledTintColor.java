package nl.dgoossens.chiselsandbits2.client.render.color;

import net.minecraft.client.Minecraft;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

/**
 * General parent for all items that need colouring based on bit type.
 */
public class ChiseledTintColor {
    /**
     * Get the tint colour to display based on the bit type embedded in the tint.
     */
    public int getColor(int tint) {
        if (VoxelType.isColoured(tint))
            return BitUtil.getColourState(tint).hashCode();

        if (VoxelType.isFluid(tint)) {
            final IFluidState fstate = BitUtil.getFluidState(tint);
            FluidStack f = new FluidStack(fstate.getFluid(), 1);
            return fstate.getFluid().getAttributes().getColor(f);
        }

        return getDefaultColor(tint);
    }

    /**
     * Get the default item colour, can be overwritten if need be for a block.
     */
    public int getDefaultColor(int tintValue) {
        return Minecraft.getInstance().getItemColors().getColor(new ItemStack(BitUtil.getBlockState(tintValue).getBlock()), 1);
    }
}
