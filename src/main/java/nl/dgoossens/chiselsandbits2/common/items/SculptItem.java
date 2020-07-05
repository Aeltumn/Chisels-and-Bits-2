package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.api.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.StandardTypedItem;
import nl.dgoossens.chiselsandbits2.api.item.attributes.BitModifyItem;
import nl.dgoossens.chiselsandbits2.api.item.property.StateProperty;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeTypes;
import nl.dgoossens.chiselsandbits2.common.impl.item.MenuAction;

import java.util.HashSet;
import java.util.Set;

/**
 * An item that can sculpt by placing or extracting bits from blocks.
 */
public class SculptItem extends StandardTypedItem implements BitModifyItem {
    protected int PROPERTY_PLACEMENT; //false = place, true = swap

    public SculptItem(Item.Properties builder) {
        super(builder);

        PROPERTY_PLACEMENT = addProperty(new StateProperty(false));
    }

    public boolean isPlacing(final ItemStack stack) {
        return !getProperty(PROPERTY_PLACEMENT, Boolean.class).get(stack);
    }

    public boolean isSwapping(final ItemStack stack) {
        return getProperty(PROPERTY_PLACEMENT, Boolean.class).get(stack);
    }

    public void setSwap(final PlayerEntity player, final ItemStack stack, final boolean value) {
        getProperty(PROPERTY_PLACEMENT, Boolean.class).set(player, stack, value);
    }

    @Override
    public boolean canPerformModification(ModificationType type) {
        return type == ModificationType.BUILD || type == ModificationType.EXTRACT;
    }

    @Override
    public boolean showIconInHotbar() {
        return true;
    }

    @Override
    public ItemModeType getAssociatedType() {
        return ItemModeTypes.CHISEL;
    }

    @Override
    public String getHighlightTip(ItemStack item, String displayName) {
        return super.getHighlightTip(item, displayName) + " - " + (isSwapping(item) ? MenuAction.SWAP.getLocalizedName() : MenuAction.PLACE.getLocalizedName());
    }

    @Override
    public Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        Set<ItemModeMenu.MenuButton> ret = new HashSet<>();
        if (isSwapping(item))
            ret.add(new ItemModeMenu.MenuButton(MenuAction.PLACE, -ItemModeMenu.TEXT_DISTANCE - 18, -20, Direction.WEST));
        else
            ret.add(new ItemModeMenu.MenuButton(MenuAction.SWAP, -ItemModeMenu.TEXT_DISTANCE - 18, -20, Direction.WEST));
        return ret;
    }

    ;
}
