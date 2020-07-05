package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IItemMenu;
import nl.dgoossens.chiselsandbits2.api.item.IItemMode;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.attributes.IItemScrollWheel;
import nl.dgoossens.chiselsandbits2.api.item.attributes.PropertyOwner;
import nl.dgoossens.chiselsandbits2.api.item.property.ItemModeProperty;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

import static nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode.values;

public abstract class TypedItem extends PropertyOwner implements IItemScrollWheel, IItemMenu {
    protected int PROPERTY_ITEMMODE;

    public TypedItem(Item.Properties builder) {
        super(builder);

        PROPERTY_ITEMMODE = addProperty(new ItemModeProperty(getAssociatedType()));
    }

    /**
     * Get the selected mode for this item.
     */
    public IItemMode getSelectedMode(final ItemStack stack) {
        return getProperty(PROPERTY_ITEMMODE, IItemMode.class).get(stack);
    }

    /**
     * Set the selected item mode.
     */
    public void setSelectedMode(final PlayerEntity player, final ItemStack stack, final IItemMode mode) {
        getProperty(PROPERTY_ITEMMODE, IItemMode.class).set(player, stack, mode);
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

    //Internal method used by the chiseled block item.
    static boolean scroll(final PlayerEntity player, final ItemStack stack, final double dwheel, final IItemMode selected, final IItemModeType type) {
        if (!ChiselsAndBits2.getInstance().getConfig().enableModeScrolling.get()) return false;

        if (!(selected instanceof ItemMode)) return false;
        int offset = ((ItemMode) selected).ordinal();
        do {
            offset += (dwheel < 0 ? -1 : 1);
            if (offset >= values().length) offset = 0;
            if (offset < 0) offset = values().length - 1;
        } while (ItemMode.values()[offset].getType() != type);
        ItemPropertyUtil.setItemMode(player, stack, ItemMode.values()[offset]);
        return true;
    }
}
