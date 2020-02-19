package nl.dgoossens.chiselsandbits2.common.integration;

/*import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;*/

//@JeiPlugin
public class JEIIntegration { //implements IModPlugin {
    /*@Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "main");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Registration m = ChiselsAndBits2.getInstance().getRegister();
        addDescriptions(registration, m.CHISEL.get(), m.TAPE_MEASURE.get(), m.WRENCH.get(), m.BIT_BAG.get(), m.MORPHING_BIT.get());
    }

    private void addDescriptions(IRecipeRegistration registration, Item... items) {
        IIngredientType<ItemStack> i = registration.getIngredientManager().getIngredientType(ItemStack.class);
        for (Item it : items) {
            if(it == null) continue;
            registration.addIngredientInfo(new ItemStack(it), i, "jei." + it.getTranslationKey());
        }
    }*/
}
