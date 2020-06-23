package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class BlockStatesGenerator extends BlockStateProvider {
    public BlockStatesGenerator(DataGenerator dataGenerator, ExistingFileHelper existingFileHandler) {
        super(dataGenerator, ChiselsAndBits2.MOD_ID, existingFileHandler);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Block States";
    }

    @Override
    protected void registerStatesAndModels() {
        Block chiseledBlock =  ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        getVariantBuilder(chiseledBlock).partialState().setModels(new ConfiguredModel(models().getExistingFile(path(chiseledBlock))));
    }

    private ResourceLocation path(Block block) {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "block/"+block.getRegistryName().getPath());
    }
}
