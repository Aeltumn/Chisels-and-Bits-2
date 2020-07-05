package nl.dgoossens.chiselsandbits2.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.data.AdvancementProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

public class AdvancementsGenerator extends AdvancementProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private DataGenerator generator;

    public AdvancementsGenerator(DataGenerator generator) {
        super(generator);
        this.generator = generator;
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Advancements";
    }

    @Override
    public void act(DirectoryCache cache) throws IOException {
        final Registration reg = ChiselsAndBits2.getInstance().getRegister();

        // First time you get the chisel we give you an advancement
        Advancement craftChisel = register(cache, "craft_chisel", "story", "minecraft:story/iron_tools", reg.CHISEL.get(), FrameType.TASK, true, true, false, InventoryChangeTrigger.Instance.forItems(reg.CHISEL.get()));
        register(cache, "store_bits", "story", craftChisel, reg.BIT_BAG.get(), FrameType.TASK, true, true, false, InventoryChangeTrigger.Instance.forItems(ItemPredicate.Builder.create().tag(reg.BIT_CONTAINERS).build()));
    }

    /**
     * Optional method to register an advancement with a vanilla advancement as a parent.
     * See {@link #register(DirectoryCache, String, String, Advancement, Item, FrameType, boolean, boolean, boolean, CriterionInstance, Function)}'s javadocs for more
     * information.
     */
    private Advancement register(DirectoryCache cache, String id, String section, String parentId, Item display, FrameType frameType, boolean showToast, boolean announceToChat, boolean hidden, CriterionInstance criterion) throws IOException {
        return register(cache, id, section, null, display, frameType, showToast, announceToChat, hidden, criterion, (je) -> {
            // Because there isn't a proper advancement datagen yet we have to post-process the generated json
            // to add the parent advancement.
            if (je.isJsonObject()) {
                JsonObject obj = je.getAsJsonObject();
                obj.addProperty("parent", parentId);
                return obj;
            }
            return je;
        });
    }

    /**
     * Variant of {@link #register(DirectoryCache, String, String, Advancement, Item, FrameType, boolean, boolean, boolean, CriterionInstance, Function)} without post processing function.
     * See its documentation for more information.
     */
    private Advancement register(DirectoryCache cache, String id, String section, Advancement parent, Item display, FrameType frameType, boolean showToast, boolean announceToChat, boolean hidden, CriterionInstance criterion) throws IOException {
        return register(cache, id, section, ChiselsAndBits2.MOD_ID + ":" + section + "/" + parent.getId().getPath(), display, frameType, showToast, announceToChat, hidden, criterion);
    }

    /**
     * Custom method for registering an advancement.
     *
     * @param id          The id of this advancement, e.g. "collect_chisel" or "dye_bag"
     * @param section     The name of the section to put this advancement in, e.g. "story" or "end"
     * @param parent      Parent advancement, may be null if there is no parent or if a vanilla advancement is the parent.
     * @param criterion   Criterion for this advancement to be given to the player.
     * @param postProcess Optional post-processing value, can be used to add vanilla advancement as parent.
     * @return Generated advancement, instance can be used as parent for other advancement.
     */
    private Advancement register(DirectoryCache cache, String id, String section, @Nullable Advancement parent, Item display, FrameType frameType, boolean showToast, boolean announceToChat, boolean hidden, CriterionInstance criterion, Function<JsonElement, JsonElement> postProcess) throws IOException {
        Advancement advancement = Advancement.Builder.builder().withParent(parent)
                .withDisplay(display, new TranslationTextComponent("advancements." + section + "." + id + ".title"),
                        new TranslationTextComponent("advancements." + section + "." + id + ".description"), null,
                        frameType, showToast, announceToChat, hidden)
                .withCriterion("criterion", criterion)
                .build(new ResourceLocation(ChiselsAndBits2.MOD_ID, id));

        Path path1 = this.generator.getOutputFolder().resolve("data/" + ChiselsAndBits2.MOD_ID + "/advancements/" + section + "/" + id + ".json");
        JsonElement jsonElement = advancement.copy().serialize();
        if (postProcess != null)
            jsonElement = postProcess.apply(jsonElement);
        IDataProvider.save(GSON, cache, jsonElement, path1);
        return advancement;
    }
}
