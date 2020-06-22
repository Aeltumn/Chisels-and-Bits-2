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
        generator.addProvider(new Recipes(generator));
        generator.addProvider(new Advancements(generator));
        generator.addProvider(new ItemModels(generator, e.getExistingFileHelper()));
        generator.addProvider(new BlockModels(generator, e.getExistingFileHelper()));
        generator.addProvider(new BlockStates(generator, e.getExistingFileHelper()));

        //TODO add tag data generator
    }
}
