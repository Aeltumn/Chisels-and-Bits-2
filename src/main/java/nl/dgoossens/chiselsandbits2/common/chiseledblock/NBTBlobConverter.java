package nl.dgoossens.chiselsandbits2.common.chiseledblock;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.blocks.ChiseledBlockTileEntity;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlobStateReference;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;
import nl.dgoossens.chiselsandbits2.common.util.ChiselUtil;

public class NBTBlobConverter {
    public static final String NBT_VERSIONED_VOXEL = "X";

    private ChiseledBlockTileEntity tile;
    private VoxelBlobStateReference voxelBlobRef;
    private int format = -1;

    public NBTBlobConverter() {
    }

    public NBTBlobConverter(final ChiseledBlockTileEntity tile) {
        this.tile = tile;

        if (tile != null) {
            voxelBlobRef = tile.getVoxelReference();
            format = voxelBlobRef == null ? -1 : voxelBlobRef.getFormat();
        }
    }

    public VoxelBlobStateReference getVoxelRef(final int version) {
        final VoxelBlobStateReference voxelRef = getReference();
        if (format == version)
            return new VoxelBlobStateReference(voxelRef.getByteArray());
        return new VoxelBlobStateReference(voxelRef.getVoxelBlob().write(version));
    }

    public void setBlob(final VoxelBlob vb) {
        voxelBlobRef = new VoxelBlobStateReference(vb);
        format = voxelBlobRef.getFormat();
    }

    public final void writeChiselData(final CompoundNBT compound) {
        final VoxelBlobStateReference voxelRef = getReference();

        final int newFormat = VoxelVersions.getDefault();
        final byte[] voxelBytes = newFormat == format ? voxelRef.getByteArray() : voxelRef.getVoxelBlob().write(newFormat);

        compound.putByteArray(NBT_VERSIONED_VOXEL, voxelBytes);
    }

    public final boolean readChiselData(final CompoundNBT compound, final int preferredFormat) {
        if (compound == null || !compound.contains(NBT_VERSIONED_VOXEL)) {
            voxelBlobRef = new VoxelBlobStateReference();
            format = voxelBlobRef.getFormat();

            if (tile != null)
                return tile.updateBlob(this);
            return false;
        }

        byte[] v = compound.getByteArray(NBT_VERSIONED_VOXEL);
        voxelBlobRef = new VoxelBlobStateReference(v);
        format = voxelBlobRef.getFormat();

        boolean formatChanged = false;
        if (preferredFormat != format && preferredFormat != VoxelVersions.ANY.getId()) {
            formatChanged = true;
            v = voxelBlobRef.getVoxelBlob().write(preferredFormat);
            voxelBlobRef = new VoxelBlobStateReference(v);
            format = voxelBlobRef.getFormat();
        }

        if (tile != null) {
            if (formatChanged) {
                // this only works on already loaded tiles, so i'm not sure
                // there is much point in it.
                tile.markDirty();
            }

            return tile.updateBlob(this);
        }
        return true;
    }

    public ItemStack getItemStack() {
        final Block blk = ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        final ItemStack is = new ItemStack(blk);
        final CompoundNBT compound = is.getOrCreateChildTag(ChiselUtil.NBT_BLOCKENTITYTAG);
        writeChiselData(compound);
        if (!compound.isEmpty()) return is;
        return null;
    }

    public VoxelBlobStateReference getReference() {
        if (voxelBlobRef == null) voxelBlobRef = new VoxelBlobStateReference();
        return voxelBlobRef;
    }

    public VoxelBlob getVoxelBlob() {
        return getReference().getVoxelBlob();
    }
}
