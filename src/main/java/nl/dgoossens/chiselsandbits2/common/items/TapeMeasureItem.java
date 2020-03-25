package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.property.ColourProperty;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import nl.dgoossens.chiselsandbits2.common.impl.item.ItemModeType;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;
import nl.dgoossens.chiselsandbits2.client.util.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TapeMeasureItem extends TypedItem {
    protected int PROPERTY_COLOR;
    public TapeMeasureItem(Properties builder) {
        super(builder);

        PROPERTY_COLOR = addProperty(new ColourProperty(DyedItemColour.WHITE));
    }

    public DyedItemColour getColour(final ItemStack stack) {
        return getProperty(PROPERTY_COLOR, DyedItemColour.class).get(stack);
    }

    public void setColour(final PlayerEntity player, final ItemStack stack, final DyedItemColour value) {
        getProperty(PROPERTY_COLOR, DyedItemColour.class).set(player, stack, value);
    }

    @Override
    public boolean showIconInHotbar() {
        return false;
    }

    @Override
    public String getHighlightTip(ItemStack item, String displayName) {
        return super.getHighlightTip(item, displayName) + " - " + getColour(item).getLocalizedName();
    }

    @Override
    public IItemModeType getAssociatedType() {
        return ItemModeType.TAPEMEASURE;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "tape_measure.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu
        );
    }

    @Override
    public Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        Set<ItemModeMenu.MenuButton> ret = new HashSet<>();
        final int colorSize = DyeColor.values().length / 4 * 24 - 4;
        double underring = -ItemModeMenu.RING_OUTER_EDGE - 34;
        double bntPos = -colorSize;
        final int bntSize = 24;
        Direction textSide = Direction.UP;
        for (final DyedItemColour color : DyedItemColour.values()) {
            if (bntPos > colorSize) {
                underring = ItemModeMenu.RING_OUTER_EDGE;
                bntPos = -colorSize;
                textSide = Direction.DOWN;
            }
            ret.add(new ItemModeMenu.MenuButton(color.getLocalizedName(), bntPos, underring, color.getColour(), textSide, () -> ClientItemPropertyUtil.setTapeMeasureColor(color)));
            bntPos += bntSize;
        }
        return ret;
    }
}
