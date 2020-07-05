package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.api.item.attributes.ItemScrollWheel;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;

import java.util.Set;

/**
 * An interface fpr any item that can have options in the radial menu.
 */
public interface TypedItem extends ItemScrollWheel {
    /**
     * The type of this item, used to automatically determine
     * the contents of the menu.
     */
    ItemModeType getAssociatedType();

    /**
     * Should this typed item show the selected type as a preview in the hotbar?
     */
    boolean showIconInHotbar();

    /**
     * Get a set of all menu buttons this item should have in the radial menu.
     * Undo/redo are always present regardless of this set.
     * <p>
     * Returns null by default.
     */
    public default Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        return null;
    }
}
