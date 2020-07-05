package nl.dgoossens.chiselsandbits2.common.network.server;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

import java.util.function.Supplier;

/**
 * Starts/ends an undo group.
 * Sent SERVER -> CLIENT.
 */
public class SGroupMethod {
    public static class BeginGroupPacket {
        public static void encode(BeginGroupPacket msg, PacketBuffer buf) {
        }

        public static BeginGroupPacket decode(PacketBuffer buffer) {
            return new BeginGroupPacket();
        }

        public static void handle(final BeginGroupPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    ChiselsAndBits2.getInstance().getUndoTracker().beginGroup(ChiselsAndBits2.getInstance().getClient().getPlayer())
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static class EndGroupPacket {
        public static void encode(EndGroupPacket msg, PacketBuffer buf) {
        }

        public static EndGroupPacket decode(PacketBuffer buffer) {
            return new EndGroupPacket();
        }

        public static void handle(final EndGroupPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    ChiselsAndBits2.getInstance().getUndoTracker().endGroup(ChiselsAndBits2.getInstance().getClient().getPlayer())
            );
            ctx.get().setPacketHandled(true);
        }
    }
}
