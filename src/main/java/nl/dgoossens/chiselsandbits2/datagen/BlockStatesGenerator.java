package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;

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
        Registration reg = ChiselsAndBits2.getInstance().getRegister();
        Block chiseledBlock = reg.CHISELED_BLOCK.get();
        getVariantBuilder(chiseledBlock).partialState().setModels(new ConfiguredModel(models().getExistingFile(path(chiseledBlock))));
        Block previewBlock = reg.PREVIEW_BLOCK.get();
        getVariantBuilder(previewBlock).partialState().setModels(new ConfiguredModel(models().getExistingFile(path(previewBlock))));
    }

    private ResourceLocation path(Block block) {
        return new ResourceLocation(ChiselsAndBits2.MOD_ID, "block/" + block.getRegistryName().getPath());
    }
}
