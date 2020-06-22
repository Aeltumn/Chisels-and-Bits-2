package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.data.ShapelessRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.Tags;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;

import java.util.function.Consumer;

public class Recipes extends RecipeProvider {
    public Recipes(DataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Recipes";
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        Registration m = ChiselsAndBits2.getInstance().getRegister();
        ShapedRecipeBuilder.shapedRecipe(m.CHISEL.get())
                .key('T', Tags.Items.INGOTS_GOLD)
                .key('S', Tags.Items.RODS_WOODEN)
                .patternLine("TS")
                .addCriterion("obtain_gold", InventoryChangeTrigger.Instance.forItems(Items.GOLD_INGOT))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(m.BIT_BAG.get())
                .key('W', ItemTags.WOOL)
                .key('T', Tags.Items.INGOTS_GOLD)
                .patternLine("WWW")
                .patternLine("WTW")
                .patternLine("WWW")
                .addCriterion("obtain_chisel", InventoryChangeTrigger.Instance.forItems(m.CHISEL.get()))
                .build(consumer);

        ShapelessRecipeBuilder.shapelessRecipe(m.MORPHING_BIT.get())
                .addIngredient(Items.ENDER_PEARL)
                .addIngredient(Tags.Items.INGOTS_GOLD)
                .addIngredient(Tags.Items.DUSTS_REDSTONE)
                .addCriterion("obtain_chisel", InventoryChangeTrigger.Instance.forItems(m.CHISEL.get()))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(m.TAPE_MEASURE.get())
                .key('I', Tags.Items.INGOTS_IRON)
                .key('Y', Tags.Items.DYES_YELLOW)
                .key('S', Items.STRING)
                .patternLine("  S")
                .patternLine("ISY")
                .patternLine("II ")
                .addCriterion("obtain_chisel", InventoryChangeTrigger.Instance.forItems(m.CHISEL.get()))
                .build(consumer);

        ShapedRecipeBuilder.shapedRecipe(m.WRENCH.get())
                .key('I', Tags.Items.INGOTS_IRON)
                .key('S', Tags.Items.RODS_WOODEN)
                .key('P', ItemTags.PLANKS)
                .patternLine(" I ")
                .patternLine(" PI")
                .patternLine("S  ")
                .addCriterion("obtain_chisel", InventoryChangeTrigger.Instance.forItems(m.CHISEL.get()))
                .build(consumer);
    }
}
