package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;
import nl.dgoossens.chiselsandbits2.common.items.TapeMeasureItem;

import java.util.function.Supplier;

/**
 * Send to set the tape measure colour
 * Sent CLIENT -> SERVER.
 */
public class CTapeMeasureColourPacket {
    private DyedItemColour state;

    private CTapeMeasureColourPacket() {
    }

    public CTapeMeasureColourPacket(final DyedItemColour state) {
        this.state = state;
    }

    public static void encode(CTapeMeasureColourPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.state.ordinal());
    }

    public static CTapeMeasureColourPacket decode(PacketBuffer buffer) {
        CTapeMeasureColourPacket pc = new CTapeMeasureColourPacket();
        pc.state = DyedItemColour.values()[buffer.readVarInt()];
        return pc;
    }

    public static void handle(final CTapeMeasureColourPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            ItemStack stack = player.getHeldItemMainhand();
            if (stack.getItem() instanceof TapeMeasureItem)
                ((TapeMeasureItem) stack.getItem()).setColour(player, stack, pkt.state);
        });
        ctx.get().setPacketHandled(true);
    }
}
