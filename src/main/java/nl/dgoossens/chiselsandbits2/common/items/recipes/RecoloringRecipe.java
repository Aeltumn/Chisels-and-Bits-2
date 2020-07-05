package nl.dgoossens.chiselsandbits2.common.items.recipes;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.DyedItemColour;
import nl.dgoossens.chiselsandbits2.common.items.BitBagItem;

/**
 * A recoloring recipe that allows users to change the colour of their bit bag
 * to any dye colour.
 */
public class RecoloringRecipe extends SpecialRecipe {
    public RecoloringRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World worldIn) {
        int i = 0;
        int j = 0;

        for (int k = 0; k < inv.getSizeInventory(); ++k) {
            ItemStack itemstack = inv.getStackInSlot(k);
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() instanceof BitBagItem) {
                    ++i;
                } else {
                    if (!itemstack.getItem().isIn(net.minecraftforge.common.Tags.Items.DYES)) {
                        return false;
                    }

                    ++j;
                }

                if (j > 1 || i > 1) {
                    return false;
                }
            }
        }

        return i == 1 && j == 1;
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        ItemStack itemstack = ItemStack.EMPTY;
        net.minecraft.item.DyeColor dyecolor = net.minecraft.item.DyeColor.WHITE;

        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack1 = inv.getStackInSlot(i);
            if (!itemstack1.isEmpty()) {
                Item item = itemstack1.getItem();
                if (item instanceof BitBagItem) {
                    itemstack = itemstack1;
                } else {
                    net.minecraft.item.DyeColor tmp = net.minecraft.item.DyeColor.getColor(itemstack1);
                    if (tmp != null) dyecolor = tmp;
                }
            }
        }

        ItemStack itemstack2 = itemstack.isEmpty() ? ItemStack.EMPTY : new ItemStack(ChiselsAndBits2.getInstance().getRegister().getBitBag(DyedItemColour.fromDye(dyecolor)).get());
        if (itemstack.hasTag()) {
            itemstack2.setTag(itemstack.getTag().copy());
        }

        return itemstack2;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ChiselsAndBits2.getInstance().getRegister().CRAFTING_SPECIAL_RECOLOR.get();
    }
}
