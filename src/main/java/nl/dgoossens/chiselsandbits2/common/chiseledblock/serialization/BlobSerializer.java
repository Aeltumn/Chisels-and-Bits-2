package nl.dgoossens.chiselsandbits2.common.chiseledblock.serialization;

import net.minecraft.network.PacketBuffer;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlob;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelVersions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlobSerializer implements VoxelSerializer {
    int lastState = -1;
    int lastIndex = -1;
    private int types;
    private Map<Integer, Integer> index; // deflate...
    private int[] palette; // inflate...
    private int bitsPerInt;
    private int bitsPerIntMinus1;

    public BlobSerializer() {
    }

    public void deflate(
            final VoxelBlob toDeflate) {
        final Set<Integer> entries = toDeflate.listContents();

        index = new HashMap<>(types = entries.size());
        palette = new int[types];

        int offset = 0;
        for (final int o : entries) {
            final int stateID = o;
            palette[offset] = stateID;
            index.put(stateID, offset++);
        }

        bitsPerInt = bitsPerBit();
        bitsPerIntMinus1 = bitsPerInt - 1;
    }

    public void inflate(
            final PacketBuffer toInflate) {
        types = toInflate.readVarInt();
        palette = new int[types];
        index = null;

        for (int x = 0; x < types; x++) {
            palette[x] = readStateID(toInflate);
        }

        bitsPerInt = bitsPerBit();
        bitsPerIntMinus1 = bitsPerInt - 1;
    }

    public void write(
            final PacketBuffer to) {
        // palette size...
        to.writeVarInt(palette.length);

        // write palette
        for (int x = 0; x < palette.length; x++) {
            writeStateID(to, palette[x]);
        }
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
     *
     * @param bits
     * @return stateID
     */
    public int readVoxelStateID(
            final BitStream bits) {
        int index = 0;

        for (int x = bitsPerIntMinus1; x >= 0; --x) {
            index |= bits.get() ? 1 << x : 0;
        }

        return getStateID(index);
    }

    /**
     * Write 1, to 16 bits per int into stream.
     *
     * @param stateID
     * @param stream
     */
    public void writeVoxelState(
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
