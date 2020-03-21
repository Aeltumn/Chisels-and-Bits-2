package nl.dgoossens.chiselsandbits2.common.network.server;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.network.IPacket;

import java.util.function.Function;
import java.util.function.Supplier;

public class SPlayerItemModePacket implements IPacket<SPlayerItemModePacket> {
    private PlayerItemMode mode;

    public SPlayerItemModePacket() {}
    public SPlayerItemModePacket(final PlayerItemMode cbm) {
        this.mode = cbm;
    }

    public PlayerItemMode getChiseledBlockMode() {
        return mode;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeVarInt(mode.ordinal());
    }

    @Override
    public Function<PacketBuffer, SPlayerItemModePacket> getDecoder() {
        return (buffer) -> new SPlayerItemModePacket(PlayerItemMode.values()[buffer.readVarInt()]);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            ClientItemPropertyUtil.readPlayerItemModes(this)
        );
        ctx.get().setPacketHandled(true);
    }
}
