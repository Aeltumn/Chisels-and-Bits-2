package nl.dgoossens.chiselsandbits2.common.network.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.function.Supplier;

/**
 * Sent to communicate changes to the bitstorage of an item.
 * Sent SERVER -> CLIENT.
 */
public class SSynchronizeBitStoragePacket {
    private INBT nbt;
    private int inventorySlot;

    private SSynchronizeBitStoragePacket() {
    }

    public SSynchronizeBitStoragePacket(final BitStorage bitStorage, final int inventorySlot) {
        nbt = StorageCapabilityProvider.STORAGE.writeNBT(bitStorage, null);
        this.inventorySlot = inventorySlot;
    }

    public static void encode(SSynchronizeBitStoragePacket msg, PacketBuffer buf) {
        CompoundNBT compound = new CompoundNBT();
        compound.put("data", msg.nbt);
        buf.writeCompoundTag(compound);
        buf.writeInt(msg.inventorySlot);
    }

    public static SSynchronizeBitStoragePacket decode(PacketBuffer buffer) {
        SSynchronizeBitStoragePacket pc = new SSynchronizeBitStoragePacket();
        CompoundNBT nbt = buffer.readCompoundTag();
        pc.nbt = nbt.get("data");
        pc.inventorySlot = buffer.readInt();
        return pc;
    }

    public static void handle(final SSynchronizeBitStoragePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity target;
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
                target = ChiselsAndBits2.getInstance().getClient().getPlayer();
            else
                target = ctx.get().getSender();

            target.inventory.getStackInSlot(pkt.inventorySlot).getCapability(StorageCapabilityProvider.STORAGE).ifPresent(storage -> storage.loadFromNBT(pkt.nbt));
            ItemPropertyUtil.recalculateGlobalSelectedVoxelWrapper(target);
        });
        ctx.get().setPacketHandled(true);
    }
}
