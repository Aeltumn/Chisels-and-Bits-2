package nl.dgoossens.chiselsandbits2.client.culling;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.ICullTest;
import nl.dgoossens.chiselsandbits2.api.VoxelType;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.utils.ModUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Determine Culling using the same checks
 * vanilla uses, with some overwrites for better
 * performance.
 */
public class MCCullTest extends DummyEnvironmentWorldReader implements ICullTest {
    //We cache results because minecraft's calculation is pretty heavy.
    private Map<Pair<BlockState, BlockState>, Boolean> resultCache = new HashMap<>();
    private BlockState a, b;

    @Override
    public boolean isVisible(final int myId, final int otherId) {
        //If this is air
        if (myId == otherId || myId == VoxelBlob.AIR_BIT) return false;

        switch (VoxelType.getType(myId)) {
            case FLUIDSTATE:
            case AIR:
                return true;
            case COLOURED:
                return false;
            case BLOCKSTATE: {
                final Pair<BlockState, BlockState> p = Pair.of(
                        ModUtil.getBlockState(myId),
                        VoxelType.getType(otherId) != VoxelType.BLOCKSTATE ? Blocks.AIR.getDefaultState() : ModUtil.getBlockState(otherId)
                );
                try {
                    resultCache.computeIfAbsent(p, (t) -> {
                        try {
                            a = t.getLeft();
                            b = t.getRight();
                            boolean d = Block.shouldSideBeRendered(a, this, BlockPos.ZERO, Direction.NORTH);
                            return d;
                        } catch (Exception x) {}
                        return null;
                    });
                    return resultCache.get(p);
                } catch(Exception x) {}
            }
        }
        //Backup logic in case any errors occur
        return new SolidCullTest().isVisible(myId, otherId);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.equals(BlockPos.ZERO)) return a;
        return b;
    }
}
