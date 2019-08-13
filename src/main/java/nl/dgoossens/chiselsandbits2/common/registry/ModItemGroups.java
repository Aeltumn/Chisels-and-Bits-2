package nl.dgoossens.chiselsandbits2.common.registry;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class ModItemGroups {
    /**
     * The base item group all chisels & bits items are in.
     */
    public static final ItemGroup CHISELS_AND_BITS2 = new ItemGroup("chiselsandbits2") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ChiselsAndBits2.getItems().CHISEL);
        }
    };
}
