package nl.dgoossens.chiselsandbits2.common.impl.serialization;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BitStream {
    int offset = 0;
    int bit = 0;
    int firstLiveInt = -1;
    int lastLiveInt = -1;

    int currentInt = 0;
    int intOffset = 0;

    IntBuffer output;
    ByteBuffer bytes;

    public BitStream() {
        bytes = ByteBuffer.allocate(250);
        output = bytes.asIntBuffer();
    }

    private BitStream(
            final int byteOffset,
            final ByteBuffer wrap) {
        intOffset = byteOffset / 4;
        bytes = wrap;
        output = bytes.asIntBuffer();
        currentInt = hasInt() ? output.get(0) : 0;
    }

    public static BitStream valueOf(
            final int byteOffset,
            final ByteBuffer wrap) {
        return new BitStream(byteOffset, wrap);
    }

    public void reset() {
        offset = 0;
        bit = 0;
        firstLiveInt = -1;
        lastLiveInt = -1;
        output.put(0, 0);
        currentInt = 0;
        intOffset = 0;
    }

    public byte[] toByteArray() {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        output.put(offset, currentInt);
        o.write(bytes.array(), 0, (lastLiveInt + 1) * 4);
        return o.toByteArray();
    }

    public boolean get() {
        final boolean result = (currentInt & 1 << bit) != 0;

        if (++bit >= 32) {
            ++offset;
            bit = 0;
            currentInt = hasInt() ? output.get(offset - intOffset) : 0;
        }

        return result;
    }

    private boolean hasInt() {
        return output.capacity() > offset - intOffset && offset - intOffset >= 0;
    }

    public void add(
            final boolean b) {
        if (b) {
            currentInt = currentInt | 1 << bit;
            lastLiveInt = offset;

            if (firstLiveInt == -1) {
                firstLiveInt = offset;
            }
        }

        if (++bit >= 32) {
            output.put(offset, currentInt);
            ++offset;
            bit = 0;
            currentInt = 0;

            // reset?
            if (output.capacity() <= offset) {
                final ByteBuffer ibytes = ByteBuffer.allocate(bytes.limit() + 248);
                final IntBuffer ioutput = ibytes.asIntBuffer();

                // copy...
                System.arraycopy(bytes.array(), 0, ibytes.array(), 0, bytes.capacity());

                bytes = ibytes;
                output = ioutput;
            }

            output.put(offset, 0);
        }
    }

    public int byteOffset() {
        return Math.max(firstLiveInt * 4, 0);
    }

}
