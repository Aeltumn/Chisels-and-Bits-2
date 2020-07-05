package nl.dgoossens.chiselsandbits2.api.item.attributes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * An interface to be implemented by an item with support for having custom functionality when the player scrolls the mouse
 * wheel whilst sneaking whilst holding the item.
 */
public interface ItemScrollWheel {
    /**
     * Called whenever the player scrolls with the item in their hand
     * whilst shifting.
     */
    boolean scroll(PlayerEntity player, ItemStack stack, double dwheel);
}
