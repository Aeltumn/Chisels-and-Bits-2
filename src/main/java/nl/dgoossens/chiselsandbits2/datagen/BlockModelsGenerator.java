package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class BlockModelsGenerator extends BlockModelProvider {
    public BlockModelsGenerator(DataGenerator dataGenerator, ExistingFileHelper existingFileHandler) {
        super(dataGenerator, ChiselsAndBits2.MOD_ID, existingFileHandler);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Block Models";
    }

    @Override
    protected void registerModels() {
        // The chiseled block has en empty model
        getBuilder("chiseled_block");

        // Regular cube for the preview block
        cubeAll("preview_block", new ResourceLocation(ChiselsAndBits2.MOD_ID, "block/preview_block"));
    }
}
