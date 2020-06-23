package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;

public class TagsGenerator extends ItemTagsProvider {
    public TagsGenerator(DataGenerator generator) {
        super(generator);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Tags";
    }

    @Override
    protected void registerTags() {
        Registration reg = ChiselsAndBits2.getInstance().getRegister();
        getBuilder(reg.BIT_BAGS).add(reg.BIT_BAG.get(), reg.WHITE_BIT_BAG.get(), reg.ORANGE_BIT_BAG.get(), reg.MAGENTA_BIT_BAG.get(),
                reg.LIGHT_BLUE_BIT_BAG.get(), reg.YELLOW_BIT_BAG.get(), reg.LIME_BIT_BAG.get(), reg.PINK_BIT_BAG.get(), reg.GRAY_BIT_BAG.get(),
                reg.LIGHT_GRAY_BIT_BAG.get(), reg.CYAN_BIT_BAG.get(), reg.PURPLE_BIT_BAG.get(), reg.BLUE_BIT_BAG.get(), reg.BROWN_BIT_BAG.get(),
                reg.GREEN_BIT_BAG.get(), reg.RED_BIT_BAG.get(), reg.BLACK_BIT_BAG.get());
        getBuilder(reg.BIT_CONTAINERS).add(reg.BIT_BAGS);
    }
}
