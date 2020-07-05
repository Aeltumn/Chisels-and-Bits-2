package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.common.impl.ChiselHandler;

import java.util.function.Supplier;

/**
 * Executes a wrench action.
 * Sent CLIENT -> SERVER.
 */
public class CWrenchBlockPacket {
    public BlockPos pos;
    public Direction side;

    private CWrenchBlockPacket() {
    }

    public CWrenchBlockPacket(final BlockPos pos, final Direction side) {
        this.pos = pos;
        this.side = side;
    }

    public static void encode(CWrenchBlockPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.side.ordinal());
    }

    public static CWrenchBlockPacket decode(PacketBuffer buffer) {
        CWrenchBlockPacket pc = new CWrenchBlockPacket();
        pc.pos = buffer.readBlockPos();
        pc.side = Direction.values()[buffer.readVarInt()];
        return pc;
    }

    public static void handle(final CWrenchBlockPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ChiselHandler.handle(pkt, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
