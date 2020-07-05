package nl.dgoossens.chiselsandbits2.api.voxel;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.render.CullTest;
import nl.dgoossens.chiselsandbits2.api.iterator.BitIterator;
import nl.dgoossens.chiselsandbits2.common.impl.serialization.BlobSerilizationCache;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.common.util.RotationUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.InflaterInputStream;

public final class VoxelBlob {
    public final static int AIR_BIT = 0;

    public final static int DIMENSION = 16;
    public final static int DIMENSION2 = DIMENSION * DIMENSION;
    public final static int DIMENSION3 = DIMENSION2 * DIMENSION;

    public final static int ARRAY_SIZE = DIMENSION3;
    public final static int DIMENSION_MINUS_ONE = DIMENSION - 1;

    public final static VoxelBlob NULL_BLOB = new VoxelBlob();
    public final static VoxelBlob FULL_BLOB = new VoxelBlob(BitUtil.getBlockId(ChiselsAndBits2.getInstance().getRegister().PREVIEW_BLOCK.get().getDefaultState()));

    private int best_buffer_size = 26;
    final int[] values = new int[ARRAY_SIZE];
    //Every int in the values map is used as follows:
    //  00000000000000000000000000000000

    // The integer's 2 MSB determine the type:
    // 00 -> legacy
    // 01 -> fluidstate
    // 10 -> colouredstate
    // 11 -> blockstate

    // Each type has it's own usage of the remaining 30 bits
    // Coloured State -> 30: 6 - 8 - 8 - 8
    // which leaves 64 possible alpha's and 255 for R, G and B
    // the alpha value is retrieved by multiplying the value from the 6 bits by 4

    //--- CONSTRUCTORS ---
    public VoxelBlob() {
    }

    public VoxelBlob(final VoxelBlob vb) {
        System.arraycopy(vb.values, 0, values, 0, values.length);
    }

    public VoxelBlob(final int i) {
        Arrays.fill(values, i);
    }

    public int[] getValues() {
        return values;
    }

    /**
     * Creates a VoxelBlob filled with type.
     */
    public static VoxelBlob full(final int type) {
        final VoxelBlob b = new VoxelBlob();
        Arrays.fill(b.values, type);
        return b;
    }

    /**
     * Gets a VoxelBlob filled with air.
     */
    public static VoxelBlob getAirBlob() {
        final VoxelBlob b = new VoxelBlob();
        Arrays.fill(b.values, VoxelBlob.AIR_BIT);
        return b;
    }

    /**
     * Create the default chiseled block to use in the statistics menu
     * and for newly spawned items.
     */
    public static VoxelBlob getDefaultBlob() {
        final VoxelBlob vb = new VoxelBlob();
        int b = BitUtil.getBlockId(ChiselsAndBits2.getInstance().getRegister().PREVIEW_BLOCK.get().getDefaultState());
        for (int y = 1; y <= 12; y++)
            for (int x = 2; x <= 13; x++)
                for (int z = 2; z <= 13; z++) {
                    vb.set(x, y, z, b);
                }
        return vb;
    }

    /**
     * Get the bit at a given index.
     */
    public int getIndex(final int index) {
        return values[index];
    }

    /**
     * Set the bit at a given index.
     */
    public void setIndex(final int index, final int value) {
        values[index] = value;
    }

    /**
     * Merges the second voxelblob into this one.
     * Returns this.
     */
    public VoxelBlob merge(final VoxelBlob second) {
        for (int x = 0; x < values.length; ++x)
            //If we have a spot here, place the bit from the second one here.
            if (values[x] == VoxelBlob.AIR_BIT) values[x] = second.values[x];
        return this;
    }

    /**
     * Merges the second voxelblob into this one.
     * Returns this.
     */
    public VoxelBlob overlap(final VoxelBlob second) {
        for (int x = 0; x < values.length; ++x)
            //If we have something to put here from second, place it.
            //Bits are destroyed in this process!
            if (second.values[x] != VoxelBlob.AIR_BIT) values[x] = second.values[x];
        return this;
    }

    /**
     * Returns a new VoxelBlob which contains
     * a version of this one mirrored
     * across the given axis.
     */
    public VoxelBlob mirror(final Direction.Axis axis) {
        VoxelBlob out = new VoxelBlob();
        final BitIterator bi = new BitIterator();
        Map<Integer, Integer> mappings = new HashMap<>();
        while (bi.hasNext()) {
            int i = mappings.computeIfAbsent(bi.getNext(this), (bit) -> {
                if (VoxelType.isBlock(bit)) return RotationUtil.mirrorBlockState(bit, axis);
                return bit;
            });
            if (bi.getNext(this) != AIR_BIT) {
                switch (axis) {
                    case X:
                        out.set(bi.x, bi.y, DIMENSION_MINUS_ONE - bi.z, i);
                        break;
                    case Y:
                        out.set(bi.x, DIMENSION_MINUS_ONE - bi.y, bi.z, i);
                        break;
                    case Z:
                        out.set(DIMENSION_MINUS_ONE - bi.x, bi.y, bi.z, i);
                        break;
                }
            }
        }
        return out;
    }

    /**
     * Offsets the voxel blob.
     */
    public VoxelBlob offset(final int xx, final int yy, final int zz) {
        final VoxelBlob out = new VoxelBlob();
        for (int z = 0; z < DIMENSION; z++) {
            for (int y = 0; y < DIMENSION; y++) {
                for (int x = 0; x < DIMENSION; x++)
                    out.set(x, y, z, getSafe(x - xx, y - yy, z - zz));
            }
        }
        return out;
    }

    /**
     * Spin the voxel blob along the axis by 90
     * clockwise.
     */
    public VoxelBlob spin(final Direction.Axis axis) {
        final VoxelBlob out = new VoxelBlob();
        //Rotate by 90 Degrees: x' = - y y' = x

        final BitIterator bi = new BitIterator();
        Map<Integer, Integer> mappings = new HashMap<>();
        while (bi.hasNext()) {
            int i = mappings.computeIfAbsent(bi.getNext(this), (bit) -> {
                if (VoxelType.isBlock(bit)) return RotationUtil.spinBlockState(bit, axis, false);
                return bit;
            });
            switch (axis) {
                case X: //These lines are swapped between clockwise and counterclockwise for X. As clockwise and counterclockwise were swapped somehow.
                    out.set(bi.x, DIMENSION_MINUS_ONE - bi.z, bi.y, i);
                    break;
                case Y:
                    out.set(DIMENSION_MINUS_ONE - bi.z, bi.y, bi.x, i);
                    break;
                case Z:
                    out.set(bi.y, DIMENSION_MINUS_ONE - bi.x, bi.z, i);
                    break;
                default:
                    throw new NullPointerException();
            }
        }
        return out;
    }

    /**
     * Spin the voxel blob along the axis by 90
     * counterclockwise.
     */
    public VoxelBlob spinCCW(final Direction.Axis axis) {
        final VoxelBlob out = new VoxelBlob();
        //Rotate by -90 Degrees: x' = y y' = - x

        final BitIterator bi = new BitIterator();
        Map<Integer, Integer> mappings = new HashMap<>();
        while (bi.hasNext()) {
            int i = mappings.computeIfAbsent(bi.getNext(this), (bit) -> {
                if (VoxelType.isBlock(bit)) return RotationUtil.spinBlockState(bit, axis, true);
                return bit;
            });
            switch (axis) {
                case X:
                    out.set(bi.x, bi.z, DIMENSION_MINUS_ONE - bi.y, i);
                    break;
                case Y:
                    out.set(bi.z, bi.y, DIMENSION_MINUS_ONE - bi.x, i);
                    break;
                case Z:
                    out.set(DIMENSION_MINUS_ONE - bi.y, bi.x, bi.z, i);
                    break;
                default:
                    throw new NullPointerException();
            }
        }
        return out;
    }

    /**
     * Fills this VoxelBlob with a given bit type.
     */
    public VoxelBlob fill(final int value) {
        for (int x = 0; x < ARRAY_SIZE; x++)
            values[x] = value;
        return this;
    }

    //--- LOOKUP METHODS ---

    /**
     * Clears this VoxelBlob, fills it with air.
     */
    public VoxelBlob clear() {
        for (int x = 0; x < ARRAY_SIZE; x++)
            values[x] = AIR_BIT;
        return this;
    }

    /**
     * Will return true if for every bit in the VoxelBlob
     * it is true that it is air in either or both of
     * the blobs. (no bit is set in both)
     */
    public boolean canMerge(final VoxelBlob second) {
        for (int x = 0; x < values.length; ++x)
            if (values[x] != AIR_BIT && second.values[x] != AIR_BIT) return false;

        return true;
    }

    /**
     * Returns a variant of this voxel blob with all bits removed where second
     * has a bit.
     */
    public VoxelBlob intersect(final VoxelBlob second) {
        for (int x = 0; x < values.length; ++x)
            if (second.values[x] != AIR_BIT) values[x] = AIR_BIT;

        return this;
    }

    /**
     * Removes all bits of this type from this blob.
     */
    public VoxelBlob removeBitType(final int bitType) {
        for (int x = 0; x < values.length; ++x)
            if (values[x] == bitType)
                values[x] = AIR_BIT;

        return this;
    }

    /**
     * Removes all bits of this type from this blob.
     *
     * @param limit Don't remove more than the limit worth of bits.
     */
    public VoxelBlob removeBitType(final int bitType, long limit) {
        for (int x = 0; x < values.length; ++x)
            if (values[x] == bitType && limit > 0) {
                limit--;
                values[x] = AIR_BIT;
            }

        return this;
    }

    /**
     * Get the position of the center of the shape.
     */
    public BlockPos getCenter() {
        final IntegerBox bounds = getBounds();
        return bounds != null ? new BlockPos((bounds.minX + bounds.maxX) / 2, (bounds.minY + bounds.maxY) / 2, (bounds.minZ + bounds.maxZ) / 2) : null;
    }

    /**
     * Gets the bounding box around this voxel blob.
     */
    public IntegerBox getBounds() {
        IntegerBox box = null;

        final BitIterator bi = new BitIterator();
        while (bi.hasNext()) {
            if (bi.getNext(this) != AIR_BIT) {
                if (box == null) box = new IntegerBox(15, 15, 15, 0, 0, 0);
                if(bi.x < box.minX) box.minX = bi.x;
                if(bi.y < box.minY) box.minY = bi.y;
                if(bi.z < box.minZ) box.minZ = bi.z;

                if(bi.x > box.maxX) box.maxX = bi.x;
                if(bi.y > box.maxY) box.maxY = bi.y;
                if(bi.z > box.maxZ) box.maxZ = bi.z;
            }
        }
        return box != null ? box : IntegerBox.NULL;
    }

    /**
     * Returns the amount of bits that's equal to air in this blob.
     */
    public long air() {
        int i = 0;
        for (int v : values)
            if (v == AIR_BIT) i++;
        return i;
    }

    /**
     * Returns the amount of bits that's equal to this bit type in this blob.
     */
    public long count(int bitType) {
        int i = 0;
        for (int v : values)
            if (v == bitType) i++;
        return i;
    }

    /**
     * Returns the amount of fluid bits in the block.
     */
    public long fluids() {
        int i = 0;
        for (int v : values)
            if (VoxelType.isFluid(v)) i++;
        return i;
    }

    /**
     * Returns the amount of bits that's not air.
     */
    public long filled() {
        int i = 0;
        for (int v : values)
            if (v != VoxelBlob.AIR_BIT) i++;
        return i;
    }

    /**
     * Returns {@link #AIR_BIT} if this blob is made up of several
     * bit types. Otherwise returns the bit type this is made of.
     */
    public int singleType() {
        int i = values[0];
        for (int v : values) {
            //If that value is not the same we return AIR_BIT.
            if (v != i)
                return AIR_BIT;
        }
        return i;
    }

    /**
     * Get the state id of the most common state.
     * Will return 0 if the block is empty.
     */
    public int getMostCommonStateId() {
        int answer = AIR_BIT;
        long amount = 0;
        for(Map.Entry<Integer, LongAdder> blockSums : getBlockSums().entrySet()) {
            if(blockSums.getKey() != AIR_BIT) {
                if(blockSums.getValue().longValue() > amount) {
                    answer = blockSums.getKey();
                    amount = blockSums.getValue().longValue();
                }
            }
        }
        return answer;
    }

    /**
     * Returns a set of all bit ids in this voxel blob.
     */
    public Set<Integer> listContents() {
        Set<Integer> ret = new HashSet<>();
        for(int i : values)
            ret.add(i);
        return ret;
    }

    //--- ACTION METHODS ---

    /**
     * Returns a map with bit-count sums of all bits in this
     * voxelblob.
     * LongAdder is used to allow for multithreaded counting.
     */
    public Map<Integer, LongAdder> getBlockSums() {
        final Map<Integer, LongAdder> counts = new ConcurrentHashMap<>();
        for (int f : values) {
            if (!counts.containsKey(f)) counts.put(f, new LongAdder());
            counts.get(f).increment();
        }
        return counts;
    }

    /**
     * Get the voxel type of a bit at a position.
     */
    public VoxelType getVoxelType(final int x, final int y, final int z) {
        return VoxelType.getType(get(x, y, z));
    }

    /**
     * Gets a pit at a given position.
     */
    public int get(final int x, final int y, final int z) {
        return getIndex(x | y << 4 | z << 8);
    }

    /**
     * Sets a bit at a given x/y/z to a value.
     */
    public void set(final int x, final int y, final int z, final int value) {
        setIndex(x | y << 4 | z << 8, value);
    }

    /**
     * Sets a bit to air a given position.
     */
    public void clear(final int x, final int y, final int z) {
        setIndex(x | y << 4 | z << 8, AIR_BIT);
    }

    /**
     * Get the bit at a given location. Doesn't throw errors when
     * the coordinates are not in this voxel blob.
     */
    public int getSafe(final int x, final int y, final int z) {
        if (x >= 0 && x < DIMENSION && y >= 0 && y < DIMENSION && z >= 0 && z < DIMENSION)
            return get(x, y, z);
        return AIR_BIT;
    }

    //--- OBJECT OVERWRITE METHODS ---
    @Override
    public VoxelBlob clone() {
        return new VoxelBlob(this);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof VoxelBlob) {
            final VoxelBlob a = (VoxelBlob) obj;
            return Arrays.equals(a.values, values);
        }
        return false;
    }

    //--- INTERNAL LOGIC METHODS ---

    /**
     * Updates the visible faces. Runs the cull tests to determine which faces should be culled.
     */
    public void updateFaceVisibility(final Direction face, int x, int y, int z, final VisibleFace dest, final VoxelBlob secondBlob, final CullTest cullVisTest) {
        final int mySpot = get(x, y, z);
        dest.state = mySpot;

        x += face.getXOffset();
        y += face.getYOffset();
        z += face.getZOffset();

        if (x >= 0 && x < DIMENSION && y >= 0 && y < DIMENSION && z >= 0 && z < DIMENSION) {
            dest.isEdge = false;
            dest.visibleFace = cullVisTest.isVisible(mySpot, get(x, y, z));
        } else {
            dest.isEdge = true;
            dest.visibleFace = (secondBlob == null ? (mySpot != AIR_BIT) :
                    (cullVisTest.isVisible(mySpot, secondBlob.get(x - face.getXOffset() * DIMENSION, y - face.getYOffset() * DIMENSION, z - face.getZOffset() * DIMENSION))));
        }
    }

    //--- SERIALIZATION ---

    /**
     * Reads this VoxelBlob from a byte array.
     */
    public static VoxelBlob readFromBytes(final byte[] bytes) {
        try {
            if (bytes.length < 1)
                throw new RuntimeException("Unable to load VoxelBlob: length of data was 0");
            VoxelBlob blob = new VoxelBlob();
            blob.read(new ByteArrayInputStream(bytes));
            return blob;
        } catch (Exception x) {
            throw new RuntimeException("Unable to load VoxelBlob", x);
        }
    }

    /**
     * Gets a byte array from this VoxelBlob.
     */
    public byte[] toByteArray() {
        return write(VoxelVersions.getDefault());
    }

    /**
     * Reads this VoxelBlob's values from the supplied ByteArrayInputStream.
     */
    private void read(final ByteArrayInputStream o) throws IOException, RuntimeException {
        final InflaterInputStream w = new InflaterInputStream(o);
        final ByteBuffer bb = BlobSerilizationCache.getCacheBuffer();

        int usedBytes = 0;
        int rv = 0;

        do {
            usedBytes += rv;
            rv = w.read(bb.array(), usedBytes, bb.limit() - usedBytes);
        } while (rv > 0);
        w.close();

        final PacketBuffer header = new PacketBuffer(Unpooled.wrappedBuffer(bb));
        final int version = header.readVarInt();
        VoxelVersions versions = VoxelVersions.getVersion(version);
        if (versions == VoxelVersions.ANY) throw new RuntimeException("Invalid Version: " + version);

        try {
            VoxelSerializer bs = versions.getSerializerClass();
            if (bs == null) throw new RuntimeException("Invalid VoxelVersion: " + version + ", worker was null");
            bs.read(header, this);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Creates a byte array representing this VoxelBlob.
     */
    public byte[] write(final VoxelVersions ret) {
        final ByteArrayOutputStream o = new ByteArrayOutputStream(best_buffer_size);
        if (ret == VoxelVersions.ANY) throw new RuntimeException("Invalid Version: " + ret);
        try {
            VoxelSerializer bs = ret.getSerializerClass();
            if (bs == null) return null;

            try {
                final PacketBuffer pb = BlobSerilizationCache.getCachePacketBuffer();
                pb.writeVarInt(bs.getVersion().getId());
                bs.write(pb, this, o, best_buffer_size);
                o.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            final byte[] ot = o.toByteArray();
            if (best_buffer_size < ot.length) best_buffer_size = ot.length;
            return ot;
        } catch (Exception x) {
            x.printStackTrace();
        }
        return o.toByteArray();
    }

    /**
     * Object used to cache the state of a face's visibility. Has it been culled, is it on an edge?
     */
    public static class VisibleFace {
        public boolean isEdge;
        public boolean visibleFace;
        public int state;
    }
}
