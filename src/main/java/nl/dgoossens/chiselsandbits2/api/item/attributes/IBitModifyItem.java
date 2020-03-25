package nl.dgoossens.chiselsandbits2.api.item.attributes;

import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemMode;
import nl.dgoossens.chiselsandbits2.common.util.ItemPropertyUtil;

/**
 * Interface for any item that is capable of modifying bits in the world.
 */
public interface IBitModifyItem {
    /**
     * Whether or not this item can be used to perform a given modification type.
     */
    boolean canPerformModification(final ModificationType type);

    /**
     * Performs an optional custom modification.
     * @param attack If the left mouse button was clicked, false if right mouse button.
     */
    public default void performCustomModification(final boolean attack, final ItemStack item) {}

    /**
     * Validates whether or not this item's allowed modification type can be triggered by the
     * given mouse button.
     * @param leftClick If the left mouse button was clicked, false if right mouse button.
     */
    public default boolean validateUsedButton(final ModificationType modificationType, final boolean leftClick, final ItemStack item) {
        return modificationType.validateUsedButton(leftClick, item);
    }

    /**
     * The various types of modifications that can be applied.
     */
    static enum ModificationType {
        EXTRACT, //Remove bits
        BUILD, //Place bits
        ROTATE, //Rotate blocks
        MIRROR, //Mirror block
        PLACE, //Placing chiseled blocks (off-grid)

        CUSTOM, //Unknown type of action, use an interface extending IBitModifyItem and specify it
        ;

        //Internal validation method for standard modification types
        private boolean validateUsedButton(boolean leftClick, ItemStack item) {
            switch(this) {
                case EXTRACT: return leftClick; //Left Click
                case PLACE:
                case BUILD:
                    return !leftClick; //Right Click
                case ROTATE: return ItemPropertyUtil.isItemMode(item, ItemMode.WRENCH_ROTATE, ItemMode.WRENCH_ROTATECCW) && !leftClick; //Right Click
                case MIRROR: return ItemPropertyUtil.isItemMode(item, ItemMode.WRENCH_MIRROR) && !leftClick; //Right Click
            }
            return false;
        }
    }
}
