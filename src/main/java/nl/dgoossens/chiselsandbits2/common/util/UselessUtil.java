package nl.dgoossens.chiselsandbits2.common.util;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * A util for methods that were unnecessarily marked as OnlyIn client
 * making these utility methods unusable on the server.
 */
public class UselessUtil {
    /**
     * Finds the stack or an equivalent one in the main inventory
     */
    public static int getSlotFor(PlayerInventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.mainInventory.size(); ++i) {
            if (!inventory.mainInventory.get(i).isEmpty() && stackEqualExact(stack, inventory.mainInventory.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks item, NBT, and meta if the item is not damageable
     */
    private static boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }
}
