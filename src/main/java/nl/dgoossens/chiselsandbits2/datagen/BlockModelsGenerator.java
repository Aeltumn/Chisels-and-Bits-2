package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
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
        Block chiseledBlock =  ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        getBuilder(chiseledBlock.getRegistryName().getPath());
    }
}
