package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.data.AdvancementProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;

public class Advancements extends AdvancementProvider {
    public Advancements(DataGenerator generator) {
        super(generator);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Advancements";
    }

    @Override
    public void act(DirectoryCache directoryCache) {
        //TOOD Add advancement data generators
    }
}
