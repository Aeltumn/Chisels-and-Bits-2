package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

/**
 * A general interface that can be extended by any enum that wants to register new item modes.
 */
public interface ItemModeEnum extends IItemMode {
    /**
     * Default method for enums.
     */
    String name();

    /**
     * Get the localized key from this Item Mode.
     */
    public default String getLocalizedName() {
        return I18n.format("general."+ChiselsAndBits2.MOD_ID+".itemmode." + getTypelessName());
    }

    /**
     * Get the name of this item mode as it can be stored in NBT.
     */
    public default String getName() {
        return name();
    }

    /**
     * Return this enum's name() but without the type in front.
     */
    public default String getTypelessName() {
        return name().substring(getType().name().length() + 1).toLowerCase();
    }

    /**
     * Get this item mode's type.
     */
    IItemModeType getType();

    /**
     * Returns whether or not this mode has an icon.
     */
    boolean hasIcon();

    /**
     * Returns false when a item mode should not have a hotkey.
     */
    boolean hasHotkey();
}
