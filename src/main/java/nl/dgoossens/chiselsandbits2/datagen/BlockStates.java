package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ModelFile;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class BlockStates extends BlockStateProvider {
    public BlockStates(DataGenerator dataGenerator, ExistingFileHelper existingFileHandler) {
        super(dataGenerator, ChiselsAndBits2.MOD_ID, existingFileHandler);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Block States";
    }

    @Override
    protected void registerStatesAndModels() {
        Block chiseledBlock =  ChiselsAndBits2.getInstance().getRegister().CHISELED_BLOCK.get();
        //TODO Change from unchecked model file to checked model file and add generated output to input
        // so it can find those files.
        getVariantBuilder(chiseledBlock).partialState()
                .setModels(new ConfiguredModel(new ModelFile.UncheckedModelFile(path(chiseledBlock))));
    }

    private ResourceLocation path(Block block) {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "block/"+block.getRegistryName().getPath());
    }
}
