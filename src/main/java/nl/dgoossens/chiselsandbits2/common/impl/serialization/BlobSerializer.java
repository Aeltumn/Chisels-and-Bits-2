package nl.dgoossens.chiselsandbits2.common.impl.serialization;

import net.minecraft.network.PacketBuffer;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelSerializer;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.api.voxel.VoxelVersions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class BlobSerializer implements VoxelSerializer {
    public final static int ARRAY_SIZE = 16 * 16 * 16; //This blob serializer uses this array size specifically.

    int lastState = -1;
    int lastIndex = -1;
    private int types;
    private Map<Integer, Integer> index; // deflate...
    private int[] palette; // inflate...
    private int bitsPerInt;
    private int bitsPerIntMinus1;

    public BlobSerializer() {
    }

    public void read(final PacketBuffer header, final VoxelBlob voxelBlob) {
        types = header.readVarInt();
        palette = new int[types];
        index = null;

        for (int x = 0; x < types; x++) {
            palette[x] = readStateID(header);
        }

        bitsPerInt = bitsPerBit();
        bitsPerIntMinus1 = bitsPerInt - 1;

        final ByteBuffer bb = BlobSerilizationCache.getCacheBuffer();
        final int[] values = voxelBlob.getValues();
        final int byteOffset = header.readVarInt();
        final int bytesOfInterest = header.readVarInt();

        final BitStream bits = BitStream.valueOf(byteOffset, ByteBuffer.wrap(bb.array(), header.readerIndex(), bytesOfInterest));
        for (int x = 0; x < VoxelBlob.ARRAY_SIZE; x++)
            values[x] = readVoxelStateID(bits);
    }

    public void write(final PacketBuffer pb, final VoxelBlob voxelBlob, final ByteArrayOutputStream o, final int best_buffer_size) throws IOException {
        final Set<Integer> entries = voxelBlob.listContents();

        index = new HashMap<>(types = entries.size());
        palette = new int[types];

        int offset = 0;
        for (final int o2 : entries) {
            final int stateID = o2; //Copy the object to make sure it gets unboxed and stuff.
            palette[offset] = stateID;
            index.put(stateID, offset++);
        }

        bitsPerInt = bitsPerBit();
        bitsPerIntMinus1 = bitsPerInt - 1;

        final Deflater def = BlobSerilizationCache.getCacheDeflater();
        final DeflaterOutputStream w = new DeflaterOutputStream(o, def, best_buffer_size);
        final int[] values = voxelBlob.getValues();

        // palette size...
        pb.writeVarInt(palette.length);

        // write palette
        for (int x = 0; x < palette.length; x++) {
            writeStateID(pb, palette[x]);
        }

        final BitStream set = BlobSerilizationCache.getCacheBitStream();
        for (int x = 0; x < ARRAY_SIZE; x++)
            writeVoxelState(values[x], set);

        final byte[] arrayContents = set.toByteArray();
        final int bytesToWrite = arrayContents.length;
        final int byteOffset = set.byteOffset();

        pb.writeVarInt(byteOffset);
        pb.writeVarInt(bytesToWrite - byteOffset);

        w.write(pb.array(), 0, pb.writerIndex());
        w.write(arrayContents, byteOffset, bytesToWrite - byteOffset);

        w.finish();
        w.close();
        def.reset();
    }

    protected int readStateID(
            final PacketBuffer buffer) {
        return buffer.readVarInt();
    }

    protected void writeStateID(
            final PacketBuffer buffer,
            final int key) {
        buffer.writeVarInt(key);
    }

    private int bitsPerBit() {
        final int bits = Integer.SIZE - Integer.numberOfLeadingZeros(types - 1);
        return Math.max(bits, 1);
    }

    private int getIndex(
            final int stateID) {
        if (lastState == stateID) {
            return lastIndex;
        }

        lastState = stateID;
        return lastIndex = index.get(stateID);
    }

    private int getStateID(final int indexID) {
        return palette[indexID];
    }


    /**
     * Reads 1, to 16 bits per int from stream.
     */
    private int readVoxelStateID(
            final BitStream bits) {
        int index = 0;

        for (int x = bitsPerIntMinus1; x >= 0; --x) {
            index |= bits.get() ? 1 << x : 0;
        }

        return getStateID(index);
    }

    /**
     * Write 1, to 16 bits per int into stream.
     */
    private void writeVoxelState(
            final int stateID,
            final BitStream stream) {
        final int index = getIndex(stateID);

        switch (bitsPerInt) {
            default:
                throw new RuntimeException("bitsPerInt is not valid, " + bitsPerInt);

            case 16:
                stream.add((index & 0x8000) != 0);
            case 15:
                stream.add((index & 0x4000) != 0);
            case 14:
                stream.add((index & 0x2000) != 0);
            case 13:
                stream.add((index & 0x1000) != 0);
            case 12:
                stream.add((index & 0x800) != 0);
            case 11:
                stream.add((index & 0x400) != 0);
            case 10:
                stream.add((index & 0x200) != 0);
            case 9:
                stream.add((index & 0x100) != 0);
            case 8:
                stream.add((index & 0x80) != 0);
            case 7:
                stream.add((index & 0x40) != 0);
            case 6:
                stream.add((index & 0x20) != 0);
            case 5:
                stream.add((index & 0x10) != 0);
            case 4:
                stream.add((index & 0x8) != 0);
            case 3:
                stream.add((index & 0x4) != 0);
            case 2:
                stream.add((index & 0x2) != 0);
            case 1:
                stream.add((index & 0x1) != 0);
        }
    }

    public VoxelVersions getVersion() {
        return VoxelVersions.COMPACT;
    }
}
