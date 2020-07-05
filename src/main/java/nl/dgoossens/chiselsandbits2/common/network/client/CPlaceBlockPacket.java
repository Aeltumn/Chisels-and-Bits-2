package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.api.bit.BitLocation;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.ChiselHandler;

import java.util.function.Supplier;

/**
 * Places a chiseled block.
 * Sent CLIENT -> SERVER.
 */
public class CPlaceBlockPacket {
    public BitLocation location;
    public BlockPos pos;
    public Direction side;

    private CPlaceBlockPacket() {
    }

    public CPlaceBlockPacket(final BlockPos pos, final BitLocation location, final Direction side) {
        this.pos = pos;
        this.location = location;
        this.side = side;
    }

    public static void encode(CPlaceBlockPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        CChiselBlockPacket.writeBitLoc(msg.location, buf);
        buf.writeVarInt(msg.side.ordinal());
    }

    public static CPlaceBlockPacket decode(PacketBuffer buffer) {
        CPlaceBlockPacket pc = new CPlaceBlockPacket();
        pc.pos = buffer.readBlockPos();
        pc.location = CChiselBlockPacket.readBitLoc(buffer);
        pc.side = Direction.values()[buffer.readVarInt()];
        return pc;
    }

    public static void handle(final CPlaceBlockPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ChiselHandler.handle(pkt, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
