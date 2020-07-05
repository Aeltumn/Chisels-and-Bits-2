package nl.dgoossens.chiselsandbits2.api.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.attributes.PropertyOwner;
import nl.dgoossens.chiselsandbits2.api.item.property.ItemModeProperty;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModes;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import static nl.dgoossens.chiselsandbits2.common.impl.item.ItemModes.values;

/**
 * A variant of the typed item that has multiple item modes that can be switched
 * between one of which is always selected.
 * Custom usages of this system (like selecting voxel wrappers instead of modes)
 * can be made by implementing {@link TypedItem} instead.
 */
public abstract class StandardTypedItem extends PropertyOwner implements TypedItem {
    protected int PROPERTY_ITEMMODE;

    public StandardTypedItem(Item.Properties builder) {
        super(builder);
        PROPERTY_ITEMMODE = addProperty(new ItemModeProperty(getAssociatedType()));
    }

    /**
     * Get the selected mode for this item.
     */
    public ItemMode getSelectedMode(final ItemStack stack) {
        return getProperty(PROPERTY_ITEMMODE, ItemMode.class).get(stack);
    }

    /**
     * Set the selected item mode.
     */
    public void setSelectedMode(final PlayerEntity player, final ItemStack stack, final ItemMode mode) {
        getProperty(PROPERTY_ITEMMODE, ItemMode.class).set(player, stack, mode);
    }

    /**
     * Display the mode in the highlight tip. (and color for tape measure)
     */
    @Override
    public String getHighlightTip(ItemStack item, String displayName) {
        return displayName + " - " + getSelectedMode(item).getLocalizedName();
    }

    /**
     * Scrolling on the chisel scrolls through the possible modes, alternative to the menu.
     */
    @Override
    public boolean scroll(final PlayerEntity player, final ItemStack stack, final double dwheel) {
        return scroll(player, stack, dwheel, getSelectedMode(stack), getAssociatedType());
    }

    /**
     * Implementation of mode scrolling used by all typed items.
     */
    public static boolean scroll(final PlayerEntity player, final ItemStack stack, final double dwheel, final ItemMode selected, final ItemModeType type) {
        if (!ChiselsAndBits2.getInstance().getConfig().enableModeScrolling.get()) return false;

        if (!(selected instanceof ItemModes)) return false;
        int offset = ((ItemModes) selected).ordinal();
        do {
            offset += (dwheel < 0 ? -1 : 1);
            if (offset >= values().length) offset = 0;
            if (offset < 0) offset = values().length - 1;
        } while (ItemModes.values()[offset].getType() != type);
        ItemPropertyUtil.setItemMode(player, stack, ItemModes.values()[offset]);
        return true;
    }
}
