package nl.dgoossens.chiselsandbits2.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.network.client.*;
import nl.dgoossens.chiselsandbits2.common.network.server.SAddUndoStepPacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SPlayerItemModePacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SGroupMethod;
import nl.dgoossens.chiselsandbits2.common.network.server.SRehighlightItemPacket;
import nl.dgoossens.chiselsandbits2.common.network.server.SSynchronizeBitStoragePacket;

public class NetworkRouter {
    private final String PROTOCOL_VERSION = Integer.toString(1);
    private final SimpleChannel HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChiselsAndBits2.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private int disc = 0;

    /**
     * Registers all packets.
     */
    public void init() {
        //Client
        HANDLER.registerMessage(disc++, CChiselBlockPacket.class, CChiselBlockPacket::encode, CChiselBlockPacket::decode, CChiselBlockPacket::handle);
        HANDLER.registerMessage(disc++, CUndoPacket.class, CUndoPacket::encode, CUndoPacket::decode, CUndoPacket::handle);
        HANDLER.registerMessage(disc++, CRotateItemPacket.class, CRotateItemPacket::encode, CRotateItemPacket::decode, CRotateItemPacket::handle);
        HANDLER.registerMessage(disc++, CWrenchBlockPacket.class, CWrenchBlockPacket::encode, CWrenchBlockPacket::decode, CWrenchBlockPacket::handle);
        HANDLER.registerMessage(disc++, CPlaceBlockPacket.class, CPlaceBlockPacket::encode, CPlaceBlockPacket::decode, CPlaceBlockPacket::handle);
        HANDLER.registerMessage(disc++, CItemStatePacket.class, CItemStatePacket::encode, CItemStatePacket::decode, CItemStatePacket::handle);
        HANDLER.registerMessage(disc++, CTapeMeasureColourPacket.class, CTapeMeasureColourPacket::encode, CTapeMeasureColourPacket::decode, CTapeMeasureColourPacket::handle);
        HANDLER.registerMessage(disc++, CItemModePacket.class, CItemModePacket::encode, CItemModePacket::decode, CItemModePacket::handle);
        HANDLER.registerMessage(disc++, CVoxelWrapperPacket.class, CVoxelWrapperPacket::encode, CVoxelWrapperPacket::decode, CVoxelWrapperPacket::handle);
        register(COpenBitBagPacket.class);
        register(CVoidBagPacket.class);
        register(CUpdatePlayerItemModesPacket.class);

        //Server
        HANDLER.registerMessage(disc++, SSynchronizeBitStoragePacket.class, SSynchronizeBitStoragePacket::encode, SSynchronizeBitStoragePacket::decode, SSynchronizeBitStoragePacket::handle);
        HANDLER.registerMessage(disc++, SAddUndoStepPacket.class, SAddUndoStepPacket::encode, SAddUndoStepPacket::decode, SAddUndoStepPacket::handle);
        HANDLER.registerMessage(disc++, SRehighlightItemPacket.class, SRehighlightItemPacket::encode, SRehighlightItemPacket::decode, SRehighlightItemPacket::handle);
        HANDLER.registerMessage(disc++, SGroupMethod.BeginGroupPacket.class, SGroupMethod.BeginGroupPacket::encode, SGroupMethod.BeginGroupPacket::decode, SGroupMethod.BeginGroupPacket::handle);
        HANDLER.registerMessage(disc++, SGroupMethod.EndGroupPacket.class, SGroupMethod.EndGroupPacket::encode, SGroupMethod.EndGroupPacket::decode, SGroupMethod.EndGroupPacket::handle);
        register(SPlayerItemModePacket.class);
    }

    /**
     * Easy registration method for packets  by only giving the class as long as it extends IPacket.
     */
    public <T extends IPacket<T>> void register(Class<T> packet) {
        try {
            HANDLER.registerMessage(disc++, packet, IPacket::encode, packet.newInstance().getDecoder(), IPacket::handle);
        } catch(Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Sends the packet from the CLIENT to the SERVER.
     */
    public void sendToServer(final Object packet) {
        HANDLER.sendToServer(packet);
    }

    /**
     * Send the packet from SERVER to a given players CLIENT.
     */
    public void sendTo(final Object packet, final ServerPlayerEntity player) {
        HANDLER.sendTo(packet, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }
}