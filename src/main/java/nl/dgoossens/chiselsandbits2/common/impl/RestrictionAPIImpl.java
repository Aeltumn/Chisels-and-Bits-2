package nl.dgoossens.chiselsandbits2.common.impl;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.api.bit.RestrictionAPI;
import nl.dgoossens.chiselsandbits2.client.cull.DummyBlockReader;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlock;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionAPIImpl implements RestrictionAPI {
    private static Map<Block, Boolean> supportedBlocks = new ConcurrentHashMap<>();
    private static Set<Block> testedBlocks = new HashSet<>();
    private static Map<IProperty<?>, Pair<Object, Object[]>> restrictedProperties = new HashMap<>();

    @Override
    public void setChiselable(Block block, boolean value) {
        supportedBlocks.put(block, value);
    }

    @Override
    public <T extends Comparable<T>> void restrictBlockStateProperty(IProperty<T> property, @Nullable T target, T... values) {
        restrictedProperties.put(property, Pair.of(target, values));
    }

    @Override
    public <T extends Comparable<T>> BlockState getPlacementState(BlockState block) {
        for(IProperty<?> property : restrictedProperties.keySet()) {
            if(block.has(property)) {
                Pair<Object, Object[]> dat = restrictedProperties.get(property);
                Object content = block.get(property);
                for(Object val : dat.getRight()) {
                    if(val == null) continue;
                    if(val.equals(content)) {
                        if(dat.getLeft() == null) return null; //You can't chisel here!
                        return block.with((Property<T>) property, (T) dat.getLeft());
                    }
                }
            }
        }
        return block;
    }

    @Override
    public boolean canChiselBlock(final BlockState block) {
        if (!supportedBlocks.containsKey(block.getBlock())) testBlock(block);
        return supportedBlocks.getOrDefault(block.getBlock(), false);
    }

    private void testBlock(final BlockState block) {
        //We determine if a block can be chiseled by whether or not the shape of it can be turned into a VoxelBlob.
        final Block blk = block.getBlock();
        //Don't test twice!
        if(testedBlocks.contains(blk)) return;
        testedBlocks.add(blk);

        if (blk instanceof ChiseledBlock) {
            supportedBlocks.put(blk, true);
            return;
        }
        if (blk.hasTileEntity(block)) return;
        if (!blk.getDefaultState().isSolid()) return; //TODO re-enable non-solid blocks
        //Can't be a rotatable block without being allowed to be fully rotated.
        if (blk.getDefaultState().has(BlockStateProperties.HORIZONTAL_FACING)) return;
        if (blk.getDefaultState().has(BlockStateProperties.FACING_EXCEPT_UP)) return;

        DummyBlockReader dummyWorld = new DummyBlockReader() {
            @Override
            public BlockState getBlockState(BlockPos pos) {
                if (pos.equals(BlockPos.ZERO)) return block;
                return super.getBlockState(pos);
            }
        };
        if (block.getBlockHardness(dummyWorld, BlockPos.ZERO) < 0) return; //Can't break unbreakable blocks. (they have -1 hardness)
        if (!block.getCollisionShape(dummyWorld, BlockPos.ZERO).equals(VoxelShapes.fullCube())) return; //You can only chisel blocks without a special shape.
        supportedBlocks.put(blk, true);
    }
}
