package nl.dgoossens.chiselsandbits2.common.blocks;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelTile;
import nl.dgoossens.chiselsandbits2.common.impl.voxel.TileStateImpl;
import nl.dgoossens.chiselsandbits2.common.util.VoxelNBTConverter;
import nl.dgoossens.chiselsandbits2.api.voxel.TileState;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelState;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ChiseledBlockTileEntity extends TileEntity implements VoxelTile {
    /**
     * The voxel state holds information about the voxel blob to show on this tile. Multiple tiles
     * can have the same voxel state and show the same model.
     * The voxel state is uniquely identifiable using its id and if the id of the voxel state
     * of a neighbour changes a tile can know it should re-render.
     *
     * The voxel state itself is not stored by a tile entity, the tile entity only stores
     * the id of its voxel state.
     */
    private UUID voxelStateId = VoxelState.NULL_STATE.getStateId();
    public static final ModelProperty<UUID> VOXEL_STATE_ID = new ModelProperty<>();

    /**
     * The tile state stores tile specific information like the state ids of its neighbours and
     * whether the tile should update.
     */
    private TileState tileState;
    public static final ModelProperty<TileState> TILE_STATE = new ModelProperty<>();

    public ChiseledBlockTileEntity() {
        super(ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK_TILE.get());
    }

    @Override
    public boolean hasTileState() {
        return tileState != null;
    }

    @Override
    public TileState getOrCreateTileState() {
        if (tileState == null) {
            tileState = new TileStateImpl(world, pos);
            requestModelDataUpdate();
        }
        return tileState;
    }

    @Override
    public VoxelState getVoxelState() {
        return ChiselsAndBits2.getInstance().getAPI().getVoxelManager().getVoxelState(voxelStateId).orElse(VoxelState.NULL_STATE);
    }

    @Override
    public VoxelBlob getVoxelBlob() {
        return getVoxelState().getBlob();
    }

    @Override
    public int getSafe(int x, int y, int z) {
        return getVoxelBlob().getSafe(x, y, z);
    }

    @Override
    public Optional<VoxelTile> getNeighbour(Direction direction) {
        return ChiselsAndBits2.getInstance().getAPI().getVoxelManager().getVoxelTile(getWorld(), getPos().offset(direction));
    }

    @Override
    public void updateState(final VoxelBlob voxelBlob) {
        voxelStateId = ChiselsAndBits2.getInstance().getAPI().getVoxelManager().submitNewVoxelState(voxelBlob);
        requestModelDataUpdate();
        markDirty();
        try {
            //Trigger block update
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 3);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public void completeOperation(final PlayerEntity player, final VoxelBlob vb, final boolean updateUndoTracker) {
        final VoxelBlob before = getVoxelBlob();
        //Empty voxelblob = we need to destroy this block.
        if (vb.filled() <= 0) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            if (updateUndoTracker)
                ChiselsAndBits2.getInstance().getUndoTracker().add(player, getWorld(), getPos(), before, VoxelBlob.NULL_BLOB);
            return;
        }

        //Turn to full block if made of one type.
        int singleType = vb.singleType();
        if (singleType != VoxelBlob.AIR_BIT) {
            boolean destroy = false;
            switch (VoxelType.getType(singleType)) {
                case BLOCKSTATE:
                    world.setBlockState(pos, BitUtil.getBlockState(singleType), 3);
                    destroy = true;
                    break;
                case FLUIDSTATE:
                    world.setBlockState(pos, BitUtil.getFluidState(singleType).getBlockState(), 3);
                    destroy = true;
                    break;
            }
            //Removing also needs to add to the undo tracker!
            if (destroy) {
                if (updateUndoTracker)
                    ChiselsAndBits2.getInstance().getUndoTracker().add(player, getWorld(), getPos(), before, VoxelBlob.NULL_BLOB);
                return;
            }
        }

        if (updateUndoTracker) {
            updateState(vb);
            ChiselsAndBits2.getInstance().getUndoTracker().add(player, getWorld(), getPos(), before, vb);
        } else updateState(vb);
    }

    //--- MODEL RENDERING DATA ---
    @Nonnull
    @Override
    public IModelData getModelData() {
        //We pass both the voxel and tile states to the model renderer because we
        //manually cull faces and thus need those faces to update when a neighbouring
        //block changes.
        IModelData imd = super.getModelData();
        imd.setData(VOXEL_STATE_ID, voxelStateId);
        imd.setData(TILE_STATE, getOrCreateTileState());
        return imd;
    }

    //--- UPDATE/DATA PACKETS ---
    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, -999, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        super.onDataPacket(net, pkt);
        read(pkt.getNbtCompound());
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return write(super.getUpdateTag());
    }

    @Override
    public CompoundNBT write(final CompoundNBT compound) {
        super.write(compound);

        //Write this tile's nbt data into the compound using a VoxelNBTConverter
        VoxelNBTConverter.writeToNBT(getVoxelState().getBlob(), compound);
        return compound;
    }

    @Override
    public void read(final CompoundNBT compound) {
        super.read(compound);
        updateState(VoxelNBTConverter.readFromNBT(compound));
    }
}
