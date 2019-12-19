package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

/**
 * Represents a custom action that can be performed from the radial menu of an item.
 */
public interface IMenuAction {
    /**
     * Should be true if a hotkey should be automatically generated for this menu action.
     */
    boolean hasHotkey();

    /**
     * Default method for enums, used for packet encoding.
     */
    String name();

    /**
     * Set to true if this
     */
    boolean hasIcon();

    /**
     * Execute the client-side player having triggered this menu action.
     */
    void trigger();

    /**
     * Get the resource location for the icon.
     */
    public default ResourceLocation getIconResourceLocation() {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/" + name().toLowerCase());
    }

    /**
     * Get the localized name of this menu action.
     */
    public default String getLocalizedName() {
        return I18n.format("general."+ChiselsAndBits2.MOD_ID+".menuaction." + name().toLowerCase());
    }
}
