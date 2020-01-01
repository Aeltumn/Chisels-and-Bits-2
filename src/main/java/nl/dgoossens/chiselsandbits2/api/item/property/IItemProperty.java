package nl.dgoossens.chiselsandbits2.api.item.property;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import nl.dgoossens.chiselsandbits2.api.item.attributes.PropertyOwner;

/**
 * A single item property that an item can have.
 */
public abstract class IItemProperty<T> {
    protected int slot;

    /**
     * Get the value stored in this property.
     */
    public abstract T get(final ItemStack stack);

    /**
     * Set the value to a given value. Must be called on the server
     * side.
     */
    public void set(final ItemStack stack, final T value) {
        //Ignore this whilst building the creative tab
        if(!PropertyOwner.BUILDING_CREATIVE_TAB && Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER)
            throw new UnsupportedOperationException("Can't interact with properties from client side!");
    }

    public void setSlot(int s) {
        slot = s;
    }
}