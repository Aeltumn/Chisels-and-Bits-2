package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.api.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModes;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.function.Supplier;

/**
 * Used to notify the server that the item mode is being set.
 * Sent CLIENT -> SERVER.
 */
public class CItemModePacket {
    private ItemMode state;

    private CItemModePacket() {
    }

    public CItemModePacket(final ItemMode state) {
        this.state = state;
    }

    public static void encode(CItemModePacket msg, PacketBuffer buf) {
        if (msg.state instanceof ItemModes) {
            buf.writeBoolean(true);
            buf.writeVarInt(((ItemModes) msg.state).ordinal());
        } else buf.writeBoolean(false);
    }

    public static CItemModePacket decode(PacketBuffer buffer) {
        CItemModePacket pc = new CItemModePacket();
        boolean useEnum = buffer.readBoolean();
        if (useEnum) pc.state = ItemModes.values()[buffer.readVarInt()];
        else throw new UnsupportedOperationException("We don't support addons adding item modes yet."); //TODO add support!
        return pc;
    }

    public static void handle(final CItemModePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ItemPropertyUtil.setItemMode(ctx.get().getSender(), ctx.get().getSender().getHeldItemMainhand(), pkt.state));
        ctx.get().setPacketHandled(true);
    }
}
