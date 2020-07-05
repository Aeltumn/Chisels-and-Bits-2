package nl.dgoossens.chiselsandbits2.common.impl.voxel;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelState;
import nl.dgoossens.chiselsandbits2.common.util.BlockShapeCalculator;
import nl.dgoossens.chiselsandbits2.common.util.VoxelNBTConverter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;

public class VoxelStateImpl implements VoxelState {
    private final UUID stateId = UUID.randomUUID();
    private final VoxelBlob blob;
    private int primaryBlock;
    private ItemStack itemCache;
    private VoxelShape selectionShape, collisionShape;

    public VoxelStateImpl() {
        this.blob = VoxelBlob.NULL_BLOB;
        this.primaryBlock = 0;
        this.itemCache = ItemStack.EMPTY;
        this.selectionShape = VoxelShapes.empty();
        this.collisionShape = VoxelShapes.empty();
    }

    public VoxelStateImpl(VoxelBlob blob) {
        this.blob = blob;
        this.primaryBlock = -1;
        this.itemCache = null;
        this.selectionShape = null;
        this.collisionShape = null;
    }

    @Override
    public boolean isDummy() {
        return false;
    }

    @Override
    public UUID getStateId() {
        return stateId;
    }

    @Override
    public VoxelBlob getBlob() {
        return blob;
    }

    @Override
    public int getPrimaryBlock() {
        if (primaryBlock == -1) {
            int primaryState = blob.getMostCommonStateId();
            if (VoxelType.getType(primaryState) == VoxelType.BLOCKSTATE)
                primaryBlock = primaryState;
            else
                primaryBlock = VoxelBlob.AIR_BIT;
        }
        return primaryBlock;
    }

    @Override
    public ItemStack getOrCreateItemStack() {
        if (itemCache == null) {
            if (blob.filled() == 0) {
                itemCache = ItemStack.EMPTY;
                return itemCache;
            }

            final ItemStack stack = new ItemStack(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get(), 1);
            stack.setTagInfo(VoxelNBTConverter.NBT_BLOCKENTITYTAG, VoxelNBTConverter.writeToNBT(blob));
            itemCache = stack;
        }
        return itemCache;
    }

    @Override
    public VoxelShape getSelectionShape() {
        if (selectionShape == null) {
            Pair<VoxelShape, VoxelShape> shapes = BlockShapeCalculator.calculate(blob);
            selectionShape = shapes.getKey();
            collisionShape = shapes.getValue();
        }
        return selectionShape;
    }

    @Override
    public VoxelShape getCollisionShape() {
        if (collisionShape == null) {
            Pair<VoxelShape, VoxelShape> shapes = BlockShapeCalculator.calculate(blob);
            selectionShape = shapes.getKey();
            collisionShape = shapes.getValue();
        }
        return collisionShape;
    }
}
