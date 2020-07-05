package nl.dgoossens.chiselsandbits2.common.impl.voxel;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.loading.FMLEnvironment;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelManager;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelTile;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VoxelManagerImpl implements VoxelManager {
    /**
     * Both client and server have their own map in which we store voxel states and their unique ids. This map will contain every possible
     * unique voxel blob that has been loaded this session.
     */
    private static Map<UUID, VoxelState> client = Collections.synchronizedMap(new HashMap<>()), server = Collections.synchronizedMap(new HashMap<>());
    /**
     * A map of redirected unique ids that are used when we have two identical voxel states that we didn't realise where identical at first.
     */
    private static Map<UUID, UUID> clientRedirect = Collections.synchronizedMap(new HashMap<>()), serverRedirect = Collections.synchronizedMap(new HashMap<>());

    private static Map<UUID, UUID> getSidedRedirection() {
        return FMLEnvironment.dist.isClient() ? clientRedirect : serverRedirect;
    }

    private static Map<UUID, VoxelState> getSidedMap() {
        return FMLEnvironment.dist.isClient() ? client : server;
    }

    @Override
    public Optional<VoxelTile> getVoxelTile(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof ChiseledBlockTileEntity)) return Optional.empty();
        return Optional.of((ChiseledBlockTileEntity) te);
    }

    @Override
    public Optional<VoxelState> getVoxelState(UUID stateId) {
        return Optional.ofNullable(getSidedMap().get(getSidedRedirection().getOrDefault(stateId, stateId)));
    }

    @Override
    public UUID submitNewVoxelState(VoxelBlob voxelBlob) {
        VoxelState newState = new VoxelStateImpl(voxelBlob);
        getSidedMap().put(newState.getStateId(), newState);
        return newState.getStateId();
    }
}
