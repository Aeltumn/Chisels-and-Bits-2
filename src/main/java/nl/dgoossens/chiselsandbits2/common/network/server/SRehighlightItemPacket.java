package nl.dgoossens.chiselsandbits2.common.network.server;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;

import java.util.function.Supplier;

public class SRehighlightItemPacket {
    public static void encode(SRehighlightItemPacket msg, PacketBuffer buf) {
    }

    public static SRehighlightItemPacket decode(PacketBuffer buffer) {
        return new SRehighlightItemPacket();
    }

    public static void handle(final SRehighlightItemPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ClientItemPropertyUtil::reshowHighlightedStack);
        ctx.get().setPacketHandled(true);
    }
}
