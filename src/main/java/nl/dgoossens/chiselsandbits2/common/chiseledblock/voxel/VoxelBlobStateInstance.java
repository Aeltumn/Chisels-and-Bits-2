package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.BitIterator;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.zip.InflaterInputStream;

public final class VoxelBlobStateInstance implements Comparable<VoxelBlobStateInstance> {
    protected final int hash;
    protected final byte[] voxelBytes;
    protected SoftReference<VoxelBlob> blob;

    private int format = Integer.MIN_VALUE;
    private VoxelShape selectionShape, collisionShape;

    public VoxelBlobStateInstance(final byte[] data) {
        voxelBytes = data;
        hash = Arrays.hashCode(voxelBytes);

        //TODO setup default voxel shape based on the data
        selectionShape = VoxelShapes.empty();
        collisionShape = VoxelShapes.empty();
    }

    @Override
    public boolean equals(final Object obj) {
        return compareTo((VoxelBlobStateInstance) obj) == 0;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int compareTo(final VoxelBlobStateInstance o) {
        if (o == null) return -1;

        int r = Integer.compare(hash, o.hash);
        // length
        if (r == 0)
            r = voxelBytes.length - o.voxelBytes.length;

        // for real then...
        if (r == 0) {
            for (int x = 0; x < voxelBytes.length && r == 0; x++)
                r = voxelBytes[x] - o.voxelBytes[x];
        }

        return r;
    }

    public VoxelBlob getBlob() {
        try {
            return getBlobCatchable();
        } catch (final Exception e) {
            e.printStackTrace();
            VoxelBlob vb = new VoxelBlob();
            vb.fill(BitUtil.getBlockId(Blocks.STONE.getDefaultState())); //Fill with stone by default.
            return vb;
        }
    }

    public VoxelBlob getBlobCatchable() throws Exception {
        VoxelBlob vb = blob == null ? null : blob.get();

        if (vb == null) {
            vb = new VoxelBlob();
            vb.readFromBytes(voxelBytes);
            blob = new SoftReference<>(vb);
        }
        return new VoxelBlob(vb);
    }

    public VoxelShape getCollisionShape() {
        return collisionShape;
    }

    public VoxelShape getSelectionShape() {
        return selectionShape;
    }

    public int getFormat() {
        if (format == Integer.MIN_VALUE) {
            if (voxelBytes == null || voxelBytes.length == 0)
                format = -1;
            else {
                try {
                    final InflaterInputStream arrayPeek = new InflaterInputStream(new ByteArrayInputStream(voxelBytes));
                    final byte[] peekBytes = new byte[5];
                    arrayPeek.read(peekBytes);

                    final PacketBuffer header = new PacketBuffer(Unpooled.wrappedBuffer(peekBytes));
                    format = header.readVarInt();
                } catch (final IOException e) {
                    format = 0;
                }
            }
        }
        return format;
    }
}
