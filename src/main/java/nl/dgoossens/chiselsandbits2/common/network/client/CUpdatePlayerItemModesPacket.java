package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemMode;
import nl.dgoossens.chiselsandbits2.common.impl.item.PlayerItemModeCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.network.IPacket;

import java.util.function.Function;
import java.util.function.Supplier;

public class CUpdatePlayerItemModesPacket implements IPacket<CUpdatePlayerItemModesPacket> {
    private PlayerItemMode cbm;

    public CUpdatePlayerItemModesPacket() {}
    public CUpdatePlayerItemModesPacket(final PlayerItemMode cbm) {
        this.cbm = cbm;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeVarInt(cbm.ordinal());
    }

    @Override
    public Function<PacketBuffer, CUpdatePlayerItemModesPacket> getDecoder() {
        return (buffer) -> new CUpdatePlayerItemModesPacket(PlayerItemMode.values()[buffer.readVarInt()]);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            //Set the sending player's capability
            ctx.get().getSender().getCapability(PlayerItemModeCapabilityProvider.PIMM).ifPresent(cap -> {
                cap.setChiseledBlockMode(cbm);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
