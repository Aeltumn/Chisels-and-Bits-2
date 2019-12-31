package nl.dgoossens.chiselsandbits2.api.item.attributes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.property.IItemProperty;
import nl.dgoossens.chiselsandbits2.common.utils.ClientItemPropertyUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an item that owns a property stored in its NBT.
 * List of properties C&B has:
 *  - Item Mode
 *  - Selected Bit Type (VoxelWrapper)
 *  - Locked State
 *  - Colour
 *  - Placement/Swap
 *
 *  We use a slot system to avoid having a Set of IItemProperties.
 */
public abstract class PropertyOwner extends Item {
    public static boolean BUILDING_CREATIVE_TAB = false;
    private static NullProperty NULL_PROPERTY = new NullProperty();
    private Map<Integer, IItemProperty> properties = new HashMap<>();
    private CompoundNBT lastNBT = null;

    public PropertyOwner(final Item.Properties builder) {
        super(builder);
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundNBT nbt) {
        super.readShareTag(stack, nbt);
        System.out.println("READ SHARE TAG");

        //Is this item being held?
        if(ChiselsAndBits2.getInstance().getClient().getPlayer().getHeldItemMainhand().equals(stack, false)) {
            if(lastNBT == null && nbt == null) return;
            if(lastNBT != null && lastNBT.equals(nbt)) return;
            lastNBT = nbt;
            //Reshow the item's name as it has changed now
            ClientItemPropertyUtil.reshowHighlightedStack();
        }
    }

    /**
     * Adds a new property to this item, should be called in the
     * constructor.
     * @return the slot id for this property
     */
    public int addProperty(IItemProperty property) {
        int mySlot = 0;
        while(properties.containsKey(mySlot))
            mySlot++;
        property.setSlot(mySlot);
        properties.put(mySlot, property);
        return mySlot;
    }

    /**
     * Get the property in a given slot.
     * @param returnType Optional class to set return type argument.
     */
    public <T> IItemProperty<T> getProperty(int slot, Class<T> returnType) {
        return properties.getOrDefault(slot, NULL_PROPERTY);
    }

    static class NullProperty extends IItemProperty<Object> {
        @Override
        public Object get(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, Object value) {
            super.set(stack, value);
        }
    }
}
