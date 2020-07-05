package nl.dgoossens.chiselsandbits2.api.item.attributes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.api.item.property.ItemProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an item that owns a property stored in its NBT.
 * List of properties C&B has:
 * - Item Mode
 * - Selected Bit Type (VoxelWrapper)
 * - Locked State
 * - Colour
 * - Placement/Swap
 * <p>
 * We use a slot system to avoid having a Set of IItemProperties.
 */
public abstract class PropertyOwner extends Item {
    public static boolean BUILDING_CREATIVE_TAB = false;
    private static NullProperty NULL_PROPERTY = new NullProperty();
    private Map<Integer, ItemProperty> properties = new HashMap<>();

    public PropertyOwner(final Item.Properties builder) {
        super(builder);
    }

    /**
     * Adds a new property to this item, should be called in the
     * constructor.
     *
     * @return the slot id for this property
     */
    public int addProperty(ItemProperty property) {
        int mySlot = 0;
        while (properties.containsKey(mySlot))
            mySlot++;
        property.setSlot(mySlot);
        properties.put(mySlot, property);
        return mySlot;
    }

    /**
     * Get the property in a given slot.
     *
     * @param returnType Optional class to set return type argument.
     */
    public <T> ItemProperty<T> getProperty(int slot, Class<T> returnType) {
        return properties.getOrDefault(slot, NULL_PROPERTY);
    }

    static class NullProperty extends ItemProperty<Object> {
        @Override
        public Object get(ItemStack stack) {
            return null;
        }

        @Override
        public void set(PlayerEntity player, ItemStack stack, Object value) {
            super.set(player, stack, value);
        }
    }
}
