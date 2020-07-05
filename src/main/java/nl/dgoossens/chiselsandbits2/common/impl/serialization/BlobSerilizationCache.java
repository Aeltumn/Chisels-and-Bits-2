package nl.dgoossens.chiselsandbits2.common.impl.serialization;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

public class BlobSerilizationCache {
    private static ThreadLocal<BitStream> bitbuffer = new ThreadLocal<>();
    private static ThreadLocal<Deflater> deflater = new ThreadLocal<>();
    private static ThreadLocal<ByteBuffer> buffer = new ThreadLocal<>();
    private static ThreadLocal<PacketBuffer> pbuffer = new ThreadLocal<>();

    public static BitStream getCacheBitStream() {
        BitStream bb = bitbuffer.get();

        if (bb == null) {
            bb = new BitStream();
            bitbuffer.set(bb);
        }

        bb.reset();
        return bb;
    }

    public static Deflater getCacheDeflater() {
        Deflater bb = deflater.get();

        if (bb == null) {
            bb = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.set(bb);
        }

        return bb;
    }

    public static ByteBuffer getCacheBuffer() {
        ByteBuffer bb = buffer.get();

        if (bb == null) {
            bb = ByteBuffer.allocate(3145728);
            buffer.set(bb);
        }

        return bb;
    }

    public static PacketBuffer getCachePacketBuffer() {
        PacketBuffer bb = pbuffer.get();

        if (bb == null) {
            bb = new PacketBuffer(Unpooled.buffer());
            pbuffer.set(bb);
        }

        bb.resetReaderIndex();
        bb.resetWriterIndex();

        return bb;
    }

}
