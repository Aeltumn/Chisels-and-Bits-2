package nl.dgoossens.chiselsandbits2.common.network.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.chiseledblock.voxel.VoxelBlobStateReference;

import java.util.function.Supplier;

/**
 * Adds an undo step to the client tracker.
 * Sent SERVER -> CLIENT.
 */
public class SAddUndoStepPacket {
    private BlockPos pos;
    private VoxelBlobStateReference before, after;

    private SAddUndoStepPacket() {
    }

    public SAddUndoStepPacket(final BlockPos pos, final VoxelBlobStateReference before, final VoxelBlobStateReference after) {
        this.pos = pos;
        this.before = before;
        this.after = after;
    }

    public static void encode(SAddUndoStepPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        final byte[] bef = msg.before.getByteArray();
        buf.writeVarInt(bef.length);
        buf.writeBytes(bef);
        final byte[] aft = msg.after.getByteArray();
        buf.writeVarInt(aft.length);
        buf.writeBytes(aft);
    }

    public static SAddUndoStepPacket decode(PacketBuffer buffer) {
        SAddUndoStepPacket pc = new SAddUndoStepPacket();
        pc.pos = buffer.readBlockPos();
        final int lena = buffer.readVarInt();
        final byte[] ta = new byte[lena];
        buffer.readBytes(ta);
        final int lenb = buffer.readVarInt();
        final byte[] tb = new byte[lenb];
        buffer.readBytes(tb);

        pc.before = new VoxelBlobStateReference(ta);
        pc.after = new VoxelBlobStateReference(tb);
        return pc;
    }

    public static void handle(final SAddUndoStepPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ChiselsAndBits2.getInstance().getClient().getPlayer();
            ChiselsAndBits2.getInstance().getUndoTracker().add(player, player.getEntityWorld(), pkt.pos, pkt.before, pkt.after);
        });
        ctx.get().setPacketHandled(true);
    }
}
