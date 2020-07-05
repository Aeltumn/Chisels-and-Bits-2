package nl.dgoossens.chiselsandbits2.common.network.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.items.SculptItem;
import nl.dgoossens.chiselsandbits2.common.items.MorphingBitItem;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import java.util.function.Supplier;

/**
 * Send to set the item state.
 * Sent CLIENT -> SERVER.
 */
public class CItemStatePacket {
    private boolean state;
    private boolean lock;

    private CItemStatePacket() {
    }

    public CItemStatePacket(final boolean state, final boolean lock) {
        this.state = state;
        this.lock = lock;
    }

    public static void encode(CItemStatePacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.state);
        buf.writeBoolean(msg.lock);
    }

    public static CItemStatePacket decode(PacketBuffer buffer) {
        CItemStatePacket pc = new CItemStatePacket();
        pc.state = buffer.readBoolean();
        pc.lock = buffer.readBoolean();
        return pc;
    }

    public static void handle(final CItemStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            ItemStack stack = player.getHeldItemMainhand();
            if (pkt.lock && stack.getItem() instanceof MorphingBitItem) {
                //Set the selected type of this stack too just for security sake
                ItemPropertyUtil.setSelectedVoxelWrapper(player, stack, ItemPropertyUtil.getGlobalSelectedVoxelWrapper(player), false);
                ((MorphingBitItem) stack.getItem()).setLocked(player, stack, pkt.state);

                if (!pkt.state) player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.unlocked_mb"), true);
                else player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.locked_mb"), true);
            }
            if (!pkt.lock && stack.getItem() instanceof SculptItem) {
                ((SculptItem) stack.getItem()).setSwap(player, stack, pkt.state);

                if (!pkt.state) player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.place_mode"), true);
                else player.sendStatusMessage(new TranslationTextComponent("general." + ChiselsAndBits2.MOD_ID + ".info.swap_mode"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
