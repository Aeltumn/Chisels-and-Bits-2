package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.utils.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.List;

public class MorphingBitItem extends Item {
    public MorphingBitItem(Properties builder) {
        super(builder);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "morphing_bit.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem
        );
    }
}
