package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent e) {
        DataGenerator generator = e.getGenerator();
        generator.addProvider(new RecipesGenerator(generator));
        generator.addProvider(new AdvancementsGenerator(generator));
        generator.addProvider(new ItemModelsGenerator(generator, e.getExistingFileHelper()));
        generator.addProvider(new BlockModelsGenerator(generator, e.getExistingFileHelper()));
        generator.addProvider(new BlockStatesGenerator(generator, e.getExistingFileHelper()));
        generator.addProvider(new TagsGenerator(generator));
    }
}
