package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelType;
import nl.dgoossens.chiselsandbits2.api.bit.VoxelWrapper;
import nl.dgoossens.chiselsandbits2.common.bitstorage.StorageCapabilityProvider;
import nl.dgoossens.chiselsandbits2.common.items.StorageItem;
import nl.dgoossens.chiselsandbits2.common.network.IPacket;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.function.Function;
import java.util.function.Supplier;

public class CVoidBagPacket implements IPacket<CVoidBagPacket> {
    private final static ResourceLocation NULL_LOCATION = new ResourceLocation(ChiselsAndBits2.MOD_ID, "null");
    private ResourceLocation filter;

    public CVoidBagPacket() {
    }

    public CVoidBagPacket(final ResourceLocation filter) {
        this.filter = filter;
    }

    public CVoidBagPacket(final ItemStack item) {
        if (!item.isEmpty())
            filter = Block.getBlockFromItem(item.getItem()).getRegistryName();
        else
            filter = NULL_LOCATION;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeResourceLocation(filter);
    }

    @Override
    public Function<PacketBuffer, CVoidBagPacket> getDecoder() {
        return (buf) -> new CVoidBagPacket(buf.readResourceLocation());
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (!(player.getHeldItemMainhand().getItem() instanceof StorageItem))
                return;
            player.getHeldItemMainhand().getCapability(StorageCapabilityProvider.STORAGE).ifPresent(cap -> {
                //No filter no problem
                if (filter.equals(NULL_LOCATION)) {
                    for (int i = 0; i < cap.getMaximumSlots(); i++)
                        cap.clearSlot(i);
                } else {
                    for (int i = 0; i < cap.getMaximumSlots(); i++) {
                        VoxelWrapper w = cap.getSlotContent(i);
                        //Only clear this slot if this is the item we're filtering for. (this feature is a lot less useful than in C&B1 really)
                        if (w.getType() == VoxelType.BLOCKSTATE && ((Block) w.get()).getRegistryName().equals(filter))
                            cap.clearSlot(i);
                    }
                }
                ItemPropertyUtil.updateStackCapability(player.getHeldItemMainhand(), cap, player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
