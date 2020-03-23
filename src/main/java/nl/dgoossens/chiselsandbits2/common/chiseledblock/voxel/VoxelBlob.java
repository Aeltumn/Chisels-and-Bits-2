package nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel;

import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import nl.dgoossens.chiselsandbits2.api.render.ICullTest;
import nl.dgoossens.chiselsandbits2.api.block.IVoxelSrc;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.serialization.BlobSerilizationCache;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.bit.BitIterator;
import nl.dgoossens.chiselsandbits2.common.util.BitUtil;
import nl.dgoossens.chiselsandbits2.common.util.RotationUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.zip.InflaterInputStream;

public final class VoxelBlob implements IVoxelSrc {
    public final static int AIR_BIT = 0;

    public final static int DIMENSION = 16;
    public final static int DIMENSION2 = DIMENSION * DIMENSION;
    public final static int DIMENSION3 = DIMENSION2 * DIMENSION;

    public final static int ARRAY_SIZE = DIMENSION3;
    public final static int DIMENSION_MINUS_ONE = DIMENSION - 1;

    public final static VoxelBlob NULL_BLOB = new VoxelBlob();
    public final static VoxelBlob FULL_BLOCK = new VoxelBlob(BitUtil.getBlockId(Blocks.STONE.getDefaultState())); //It doesn't matter what it is a full block of.

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

    int[] getValues() {
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
                if(VoxelType.isBlock(bit)) return RotationUtil.mirrorBlockState(bit, axis);
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
                if(VoxelType.isBlock(bit)) return RotationUtil.spinBlockState(bit, axis, false);
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
                if(VoxelType.isBlock(bit)) return RotationUtil.spinBlockState(bit, axis, true);
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
     * Fills this voxelbob
     */
    public VoxelBlob fill(final int value) {
        for (int x = 0; x < ARRAY_SIZE; x++)
            values[x] = value;
        return this;
    }

    //--- LOOKUP METHODS ---

    /**
     * Clears this voxelblob, fills it with air.
     */
    public VoxelBlob clear() {
        fill(AIR_BIT);
        return this;
    }

    /**
     * Will return true if for every bit in the voxelblob
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
            if(values[x] == bitType)
                values[x] = AIR_BIT;

        return this;
    }

    /**
     * Removes all bits of this type from this blob.
     * @param limit Don't remove more than the limit worth of bits.
     */
    public VoxelBlob removeBitType(final int bitType, long limit) {
        for (int x = 0; x < values.length; ++x)
            if(values[x] == bitType && limit > 0) {
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
        boolean found = false;
        int min_x = 15, min_y = 15, min_z = 15;
        int max_x = 0, max_y = 0, max_z = 0;

        final BitIterator bi = new BitIterator();
        while (bi.hasNext()) {
            if (bi.getNext(this) != AIR_BIT) {
                found = true;
                min_x = Math.min(min_x, bi.x);
                min_y = Math.min(min_y, bi.y);
                min_z = Math.min(min_z, bi.z);

                max_x = Math.max(max_x, bi.x);
                max_y = Math.max(max_y, bi.y);
                max_z = Math.max(max_z, bi.z);
            }
        }
        return found ? new IntegerBox(min_x, min_y, min_z, max_x, max_y, max_z) : IntegerBox.NULL;
    }

    /**
     * Returns the amount of bits that's equal to air in this blob.
     */
    public long air() {
        int i = 0;
        for(int v : values)
            if(v == AIR_BIT) i++;
        return i;
    }

    /**
     * Returns the amount of bits that's equal to this bit type in this blob.
     */
    public long count(int bitType) {
        int i = 0;
        for(int v : values)
            if(v == bitType) i++;
        return i;
    }

    /**
     * Returns the amount of fluid bits in the block.
     */
    public long fluids() {
        int i = 0;
        for(int v : values)
            if(VoxelType.isFluid(v)) i++;
        return i;
    }

    /**
     * Returns the amount of bits that's not air.
     */
    public long filled() {
        int i = 0;
        for(int v : values)
            if(v != VoxelBlob.AIR_BIT) i++;
        return i;
    }

    /**
     * Returns {@link #AIR_BIT} if this blob is made up of several
     * bit types. Otherwise returns the bit type this is made of.
     */
    public int singleType() {
        int i = values[0];
        for(int v : values) {
            //If that value is not the same we return AIR_BIT.
            if(v != i)
                return AIR_BIT;
        }
        return i;
    }

    /**
     * Get the state id of the most common state.
     * Will return 0 if the block is empty.
     */
    public int getMostCommonStateId() {
        return getBlockSums().entrySet().parallelStream()
                .filter(f -> f.getKey() != AIR_BIT) //We ignore air in the calculation.
                .max(Comparator.comparing(e -> e.getValue().intValue())).map(Entry::getKey)
                .orElse(AIR_BIT); //There needs to be handling downstream if this happens. This also means the block is empty.
    }

    /**
     * Get the state id of the most common fluid.
     * Will return 0 if the block is empty.
     * Used to determine which fluidstate the block should use.
     */
    public int getMostCommonFluid() {
        return getBlockSums().entrySet().parallelStream()
                .filter(f -> VoxelType.isFluid(f.getKey())) //We ignore non-fluids in the calculation.
                .max(Comparator.comparing(e -> e.getValue().intValue())).map(Entry::getKey)
                .orElse(AIR_BIT); //There needs to be handling downstream if this happens. This also means the block is empty.
    }

    /**
     * Returns a set of all bit ids in this voxel blob.
     */
    public Set<Integer> listContents() {
        return Arrays.stream(values).parallel().distinct().boxed().collect(Collectors.toSet());
    }

    //--- ACTION METHODS ---

    /**
     * Returns a map with bit-count sums of all bits in this
     * voxelblob.
     * LongAdder is used to allow for multithreaded counting.
     */
    public Map<Integer, LongAdder> getBlockSums() {
        final Map<Integer, LongAdder> counts = new ConcurrentHashMap<>();
        Arrays.stream(values).parallel()
                .forEach(f -> {
                    if (!counts.containsKey(f)) counts.put(f, new LongAdder());
                    counts.get(f).increment();
                });
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

    //--- STATIC CONSTRUCTION METHODS ---

    /**
     * Get the bit at a given location. Doesn't throw errors when
     * the coordinates are not in this voxel blob.
     */
    @Override
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
    public void updateFaceVisibility(final Direction face, int x, int y, int z, final VisibleFace dest, final VoxelBlob secondBlob, final ICullTest cullVisTest) {
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
     * Reads this voxelblob from a byte array.
     */
    public void readFromBytes(final byte[] bytes) {
        try {
            if (bytes.length < 1)
                throw new RuntimeException("Unable to load VoxelBlob: length of data was 0");
            read(new ByteArrayInputStream(bytes));
        } catch (Exception x) {
            throw new RuntimeException("Unable to load VoxelBlob", x);
        }
    }

    /**
     * Reads this voxelblobs values from the supplied ByteArrayInputStream.
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
            VoxelSerializer bs = versions.getWorker();
            if (bs == null) throw new RuntimeException("Invalid VoxelVersion: " + version + ", worker was null");
            bs.read(header, this);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Creates a byte array representing this blob.
     */
    public byte[] write(final int version) {
        final ByteArrayOutputStream o = new ByteArrayOutputStream(best_buffer_size);
        VoxelVersions ret = VoxelVersions.getVersion(version);
        if (ret == VoxelVersions.ANY) throw new RuntimeException("Invalid Version: " + version);
        try {
            VoxelSerializer bs = ret.getWorker();
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
