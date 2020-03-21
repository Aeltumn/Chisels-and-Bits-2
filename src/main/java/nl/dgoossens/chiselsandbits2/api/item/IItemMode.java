package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

/**
 * A generic item mode which can be
 */
public interface IItemMode {
    /**
     * Get the name of this item mode as it can be stored in NBT.
     */
    String getName();

    /**
     * Get the localized key from this Item Mode.
     */
    String getLocalizedName();

    /**
     * Return getName() but without the type in front.
     */
    String getTypelessName();

    /**
     * Get the type of this item mode.
     */
    IItemModeType getType();

    /**
     * Get the resource location for the icon.
     */
    public default ResourceLocation getIconResourceLocation() {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "icons/modes/" + getTypelessName().toLowerCase());
    }

    /**
     * How many pixels the png file for this icon is wide.
     */
    public default int getTextureWidth() {
        return 16;
    }

    /**
     * How many pixels the png file for this icon is high.
     */
    public default int getTextureHeight() {
        return 16;
    }
}
