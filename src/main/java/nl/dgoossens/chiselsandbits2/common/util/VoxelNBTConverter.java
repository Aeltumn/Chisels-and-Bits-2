package nl.dgoossens.chiselsandbits2.common.util;

import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelVersions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

/**
 * Converts between NBT and VoxelTiles.
 */
public class VoxelNBTConverter {
    /**
     * The NBT id we use to store the byte array at.
     */
    public static final String NBT_VERSIONED_VOXEL = "X";
    /**
     * The name of the nbt tag under which we store voxel data in
     * nbt compounds.
     */
    public static final String NBT_BLOCKENTITYTAG = "BlockEntityTag";

    /**
     * Writes the data in this converter into the supplied nbt compound.
     */
    public static void writeToNBT(final VoxelBlob voxelBlob, final CompoundNBT compound) {
        final byte[] voxelBytes = voxelBlob.write(VoxelVersions.getDefault());
        if (voxelBytes == null) return;
        compound.putByteArray(NBT_VERSIONED_VOXEL, voxelBytes);
    }

    /**
     * Writes the data in this converter into a newly created nbt compound.
     */
    public static CompoundNBT writeToNBT(final VoxelBlob voxelBlob) {
        CompoundNBT compound = new CompoundNBT();
        writeToNBT(voxelBlob, compound);
        return compound;
    }

    /**
     * Reads the data stored in the supplier compound into this nbt
     * converter.
     */
    public static VoxelBlob readFromNBT(final CompoundNBT compound) {
        if (compound == null || !compound.contains(NBT_VERSIONED_VOXEL)) {
            return VoxelBlob.NULL_BLOB;
        }

        byte[] v = compound.getByteArray(NBT_VERSIONED_VOXEL);
        return VoxelBlob.readFromBytes(v);
    }

    /**
     * Create a chiseled block item stack with the nbt data in this converter.
     */
    public static ItemStack convertToItemStack(final VoxelBlob blob) {
        final Block blk = ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        final ItemStack is = new ItemStack(blk);
        final CompoundNBT compound = is.getOrCreateChildTag(NBT_BLOCKENTITYTAG);
        writeToNBT(blob, compound);
        if (!compound.isEmpty()) return is;
        return null;
    }

    /**
     * Get the format of a supplied byte array of voxel data.
     */
    public static VoxelVersions getFormat(byte[] voxelBytes) {
        if (voxelBytes == null || voxelBytes.length == 0)
            return VoxelVersions.ANY;
        else {
            try {
                final InflaterInputStream arrayPeek = new InflaterInputStream(new ByteArrayInputStream(voxelBytes));
                final byte[] peekBytes = new byte[5];
                arrayPeek.read(peekBytes);
                final PacketBuffer header = new PacketBuffer(Unpooled.wrappedBuffer(peekBytes));
                return VoxelVersions.getVersion(header.readVarInt());
            } catch (final IOException e) {
                return VoxelVersions.ANY;
            }
        }
    }
}
