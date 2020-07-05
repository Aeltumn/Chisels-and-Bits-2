package nl.dgoossens.chiselsandbits2.api.bit;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import java.awt.Color;

/**
 * Represents the object of a VoxelType, can be either a Block, Fluid or Color.
 * This class serves as a bridge between these three classes which have no common
 * interface.
 */
public class VoxelWrapper<T> {
    private int id;
    private VoxelType type;

    private VoxelWrapper(Block b) {
        type = VoxelType.BLOCKSTATE;
        id = BitUtil.getBlockId(b.getDefaultState());
    }

    private VoxelWrapper(Fluid f) {
        type = VoxelType.FLUIDSTATE;
        id = BitUtil.getFluidId(f.getDefaultState());
    }

    private VoxelWrapper(Color c) {
        type = VoxelType.COLOURED;
        id = BitUtil.getColourId(c);
    }

    private VoxelWrapper(int bit) {
        type = VoxelType.getType(bit);
        id = bit;
    }

    /**
     * Simplifies this voxel wrapper to be the default state.
     */
    public VoxelWrapper simplify() {
        switch (type) {
            case BLOCKSTATE:
                return VoxelWrapper.forBlock((Block) get());
            case FLUIDSTATE:
                return VoxelWrapper.forFluid((Fluid) get());
            case COLOURED:
                return VoxelWrapper.forColor((Color) get());
            default:
                return this;
        }
    }

    /**
     * Returned the wrapped object, can be either Block, Fluid or Color.
     */
    public T get() {
        switch (type) {
            case BLOCKSTATE:
                return (T) BitUtil.getBlockState(id).getBlock();
            case FLUIDSTATE:
                return (T) BitUtil.getFluidState(id).getFluid();
            case COLOURED:
                return (T) BitUtil.getColourState(id);
            default:
                return (T) Blocks.AIR;
        }
    }

    /**
     * Check if this voxel wrapper is of the same type as this bit. Ignores blockstate properties and
     * compares result of {@link #get()}
     */
    public boolean equalsBit(int bitType) {
        return type == VoxelType.getType(bitType) && get().equals(new VoxelWrapper<>(bitType).get());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoxelWrapper) {
            VoxelWrapper o = (VoxelWrapper) obj;
            if (type != o.type) return false;
            //Use equals so we don't have multiple types of the same block in a bag.
            return get().equals(o.get());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return get().toString();
    }

    @Override
    public int hashCode() {
        return getId();
    }

    /**
     * Get the bit id for this voxel wrapper.
     */
    public int getId() {
        return id;
    }

    /**
     * Get this voxel wrapper's type.
     */
    public VoxelType getType() {
        return type;
    }

    /**
     * Return whether or nor this voxel wrapper is empty.
     * (whether or not it is air)
     */
    public boolean isEmpty() {
        return id == VoxelBlob.AIR_BIT;
    }

    /**
     * Gets the name of this selected item mode.
     */
    public String getDisplayName() {
        if (!isEmpty()) {
            switch (type) {
                case COLOURED:
                    Color c = (Color) get();
                    return "(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha() + ")";
                case BLOCKSTATE:
                    Block b = (Block) get();
                    return I18n.format("block" + "." + b.getRegistryName().getNamespace() + "." + b.getRegistryName().getPath());
                case FLUIDSTATE:
                    Fluid f = (Fluid) get();
                    return I18n.format("block" + "." + f.getRegistryName().getNamespace() + "." + f.getRegistryName().getPath());
            }
        }
        return I18n.format("general." + ChiselsAndBits2.MOD_ID + ".none");
    }

    /**
     * Get the item stack to be shown when this mode is selected.
     */
    public ItemStack getStack() {
        switch (type) {
            case BLOCKSTATE:
                return new ItemStack((Block) get());
            case FLUIDSTATE:
                return new ItemStack(((Fluid) get()).getFilledBucket());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Get the bit id that should be placed.
     * This factors in blockstate properties, like leaves will always place as 0 distance from log
     * or how logs will place rotated.
     */
    public int getPlacementBitId(BlockItemUseContext context) {
        switch (type) {
            case BLOCKSTATE:
                return BitUtil.getBlockId(((Block) get()).getStateForPlacement(context));
            default:
                return getId();
        }
    }

    /**
     * Creates an empty voxel object.
     */
    public static <T> VoxelWrapper<T> empty() {
        return new VoxelWrapper<>(0);
    }

    /**
     * Creates an voxel object around an unknown voxel type.
     */
    public static <T> VoxelWrapper<T> forAbstract(int bit) {
        return new VoxelWrapper<>(bit);
    }

    /**
     * Creates an voxel object around a given block.
     */
    public static VoxelWrapper<Block> forBlock(Block b) {
        return new VoxelWrapper<>(b);
    }

    /**
     * Creates an voxel object around a given fluid.
     */
    public static VoxelWrapper<Fluid> forFluid(Fluid f) {
        return new VoxelWrapper<>(f);
    }

    /**
     * Creates an voxel object around a given colour.
     */
    public static VoxelWrapper<Color> forColor(Color c) {
        return new VoxelWrapper<>(c);
    }
}
