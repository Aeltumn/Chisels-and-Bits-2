package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.attributes.PropertyOwner;
import nl.dgoossens.chiselsandbits2.common.network.server.SRehighlightItemPacket;

/**
 * A single item property that an item can have.
 */
public abstract class ItemProperty<T> {
    protected int slot;

    /**
     * Get the value stored in this property.
     */
    public abstract T get(final ItemStack stack);

    /**
     * Set the value to a given value. Must be called on the server
     * side.
     */
    public void set(final PlayerEntity player, final ItemStack stack, final T value) {
        //Ignore this whilst building the creative tab
        if (!PropertyOwner.BUILDING_CREATIVE_TAB && Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER)
            throw new UnsupportedOperationException("Can't interact with properties from client side!");
    }

    public void setSlot(int s) {
        slot = s;
    }

    /**
     * Update the stack by rehighlighting the stack name for the player if this is currently the
     * selected item.
     */
    protected void updateStack(final PlayerEntity player, final ItemStack stack) {
        if (PropertyOwner.BUILDING_CREATIVE_TAB) return;

        //Is this item being held?
        if (player.getHeldItemMainhand().equals(stack, true)) {
            //Reshow the item's name as it has changed now
            ChiselsAndBits2.getInstance().getNetworkRouter().sendTo(new SRehighlightItemPacket(), (ServerPlayerEntity) player);
        }
    }
}
