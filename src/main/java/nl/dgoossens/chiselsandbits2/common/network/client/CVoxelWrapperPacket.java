package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.function.Supplier;

/**
 * Transfer a voxel wrapper selection being set.
 * Sent CLIENT -> SERVER.
 */
public class CVoxelWrapperPacket {
    private VoxelWrapper state;
    private boolean timestamp;

    private CVoxelWrapperPacket() {
    }

    public CVoxelWrapperPacket(final VoxelWrapper state, final boolean timestamp) {
        this.state = state;
        this.timestamp = timestamp;
    }

    public static void encode(CVoxelWrapperPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.state.getId());
        buf.writeBoolean(msg.timestamp);
    }

    public static CVoxelWrapperPacket decode(PacketBuffer buffer) {
        CVoxelWrapperPacket pc = new CVoxelWrapperPacket();
        pc.state = VoxelWrapper.forAbstract(buffer.readVarInt());
        pc.timestamp = buffer.readBoolean();
        return pc;
    }

    public static void handle(final CVoxelWrapperPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ItemPropertyUtil.setSelectedVoxelWrapper(ctx.get().getSender(), ctx.get().getSender().getHeldItemMainhand(), pkt.state, pkt.timestamp));
        ctx.get().setPacketHandled(true);
    }
}
