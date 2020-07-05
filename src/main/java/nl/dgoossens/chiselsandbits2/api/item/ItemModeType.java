package nl.dgoossens.chiselsandbits2.api.item;

/**
 * Generic interface for a type of item mode.
 */
public interface ItemModeType {
    /**
     * Class implementing ItemModeTypes should be an enum.
     */
    String name();

    /**
     * Get the default value for this type.
     */
    ItemMode getDefault();
}
