package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;

public class ItemModelsGenerator extends ItemModelProvider {
    public ItemModelsGenerator(DataGenerator dataGenerator, ExistingFileHelper existingFileHandler) {
        super(dataGenerator, ChiselsAndBits2.MOD_ID, existingFileHandler);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Item Models";
    }

    @Override
    protected void registerModels() {
        // we use an unchecked model to make it a tile entity
        addTransforms(getBuilder("chiseled_block").parent(new ModelFile.UncheckedModelFile(new ResourceLocation("builtin/entity"))));
        addTransforms(getBuilder("morphing_bit"));

        withExistingParent("preview_block", new ResourceLocation(ChiselsAndBits2.MOD_ID, "block/preview_block"));

        withExistingParent("tool", "item/handheld")
                .transforms()
                    .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                        .rotation(0, -90, 55)
                        .translation(0, 3.5f, 0.75f)
                        .scale(0.55f, 0.55f, 0.55f)
                    .end()
                    .transform(ModelBuilder.Perspective.THIRDPERSON_LEFT)
                        .rotation(0, 90, 55)
                        .translation(0, 3.5f, 0.75f)
                        .scale(0.55f, 0.55f, 0.55f)
                    .end().end();
        ResourceLocation tool = new ResourceLocation(ChiselsAndBits2.MOD_ID, "tool");

        withExistingParent("bit_bag", "item/generated")
                .texture("layer0", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/bit_bag"));

        withExistingParent("chisel", tool)
                .texture("layer0", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/chisel"));

        withExistingParent("tape_measure", "item/generated")
                .texture("layer0", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/tape_measure"));

        withExistingParent("wrench", tool)
                .texture("layer0", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/wrench"));

        //Colored Bit Bags
        coloredBitBag("black");
        coloredBitBag("blue");
        coloredBitBag("brown");
        coloredBitBag("cyan");
        coloredBitBag("gray");
        coloredBitBag("green");
        coloredBitBag("light_blue");
        coloredBitBag("light_gray");
        coloredBitBag("lime");
        coloredBitBag("magenta");
        coloredBitBag("orange");
        coloredBitBag("pink");
        coloredBitBag("purple");
        coloredBitBag("red");
        coloredBitBag("white");
        coloredBitBag("yellow");
    }

    //Utility method to generate colored bit bag files easily
    private void coloredBitBag(String id) {
        withExistingParent(id+"_bit_bag", "item/generated")
                .texture("layer0", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/bit_bag_string"))
                .texture("layer1", new ResourceLocation(ChiselsAndBits2.MOD_ID, "item/bit_bag_dyeable"));
    }

    private void addTransforms(ItemModelBuilder builder) {
        builder.transforms()
                .transform(ModelBuilder.Perspective.GUI)
                    .rotation(30, 255, 0)
                    .translation(0, 0f, 0)
                    .scale(0.625f, 0.625f, 0.625f)
                .end()
                .transform(ModelBuilder.Perspective.GROUND)
                    .rotation(0, 0, 0)
                    .translation(0, 3, 0)
                    .scale(0.25f, 0.25f, 0.25f)
                .end()
                    .transform(ModelBuilder.Perspective.FIXED)
                    .rotation(0, 0, 0)
                    .translation(0, 0, 0)
                .scale(0.5f, 0.5f, 0.5f)
                .end()
                .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                    .rotation(75, 45, 0)
                    .translation(0, 2.5f, 0)
                    .scale(0.375f, 0.375f, 0.375f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                    .rotation(0, 45, 0)
                    .translation(0, 0, 0)
                    .scale(0.4f, 0.4f, 0.4f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                    .rotation(0, 225, 0)
                    .translation(0, 0, 0)
                    .scale(0.4f, 0.4f, 0.4f)
                .end().end();
    }
}
