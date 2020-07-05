package nl.dgoossens.chiselsandbits2.client.render.color;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class ColourableItemColor implements IItemColor {
    private int layer;

    public ColourableItemColor(int layer) {
        this.layer = layer;
    }

    @Override
    public int getColor(ItemStack stack, int tint) {
        if (tint != layer) return -1;
        return ChiselsAndBits2.getInstance().getRegister().getColoredItemColor(stack);
    }
}
