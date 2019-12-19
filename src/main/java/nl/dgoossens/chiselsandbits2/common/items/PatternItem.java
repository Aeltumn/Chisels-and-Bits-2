package nl.dgoossens.chiselsandbits2.common.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IItemModeType;
import nl.dgoossens.chiselsandbits2.api.item.IRotatableItem;
import nl.dgoossens.chiselsandbits2.client.gui.ItemModeMenu;
import nl.dgoossens.chiselsandbits2.common.impl.ItemModeType;
import nl.dgoossens.chiselsandbits2.common.impl.MenuAction;
import nl.dgoossens.chiselsandbits2.common.utils.ItemTooltipWriter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternItem extends TypedItem implements IRotatableItem {
    public PatternItem(Properties builder) {
        super(builder);
    }

    @Override
    public boolean showIconInHotbar() {
        return true;
    }

    @Override
    public IItemModeType getAssociatedType() {
        return ItemModeType.PATTERN;
    }

    @Override
    public Set<ItemModeMenu.MenuButton> getMenuButtons(final ItemStack item) {
        Set<ItemModeMenu.MenuButton> ret = new HashSet<>();
        ret.add(new ItemModeMenu.MenuButton(MenuAction.ROLL_X, -ItemModeMenu.TEXT_DISTANCE - 18, -20, Direction.WEST));
        ret.add(new ItemModeMenu.MenuButton(MenuAction.ROLL_Z, -ItemModeMenu.TEXT_DISTANCE - 18, 4, Direction.WEST));
        return ret;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ItemTooltipWriter.addItemInformation(tooltip, "pattern.help",
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                Minecraft.getInstance().gameSettings.keyBindUseItem,
                ChiselsAndBits2.getInstance().getKeybindings().modeMenu
        );
    }

    @Override
    public void rotate(ItemStack item, Direction.Axis axis) {

    }
}
