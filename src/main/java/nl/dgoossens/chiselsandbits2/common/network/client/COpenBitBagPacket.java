package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.api.bit.BitStorage;
import nl.dgoossens.chiselsandbits2.common.bitstorage.BagContainer;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.items.BitBagItem;
import nl.dgoossens.chiselsandbits2.common.network.IPacket;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Used to notify the server that a bit bag was opened.
 * Sent CLIENT -> SERVER.
 */
public class COpenBitBagPacket implements IPacket<COpenBitBagPacket> {
    public COpenBitBagPacket() {
    }

    @Override
    public void encode(PacketBuffer buf) {
    }

    @Override
    public Function<PacketBuffer, COpenBitBagPacket> getDecoder() {
        return (buffer) -> new COpenBitBagPacket();
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ItemStack item = ctx.get().getSender().getHeldItemMainhand();
            if (!(item.getItem() instanceof BitBagItem)) return;
            int maxSize = item.getCapability(StorageCapabilityProvider.STORAGE).map(BitStorage::getMaximumSlots).orElse(12);
            int size = item.getCapability(StorageCapabilityProvider.STORAGE).map(BitStorage::getOccupiedSlotCount).orElse(0);
            ctx.get().getSender().openContainer(new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return item.getDisplayName();
                }

                @Nullable
                @Override
                public Container createMenu(int windowId, PlayerInventory playerInv, PlayerEntity player) {
                    return new BagContainer(windowId, player, new Inventory(Math.min(size + 1, maxSize)), item);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
