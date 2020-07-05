package nl.dgoossens.chiselsandbits2.datagen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.LootTableProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootParameterSet;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraft.world.storage.loot.ValidationTracker;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LootGenerator extends LootTableProvider {
    public LootGenerator(DataGenerator generator) {
        super(generator);
    }

    @Override
    public String getName() {
        return "Chisels & Bits 2: Loot";
    }

    private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> tables =
            ImmutableList.of(Pair.of(BlockLootGenerator::new, LootParameterSets.BLOCK));

    @Override
    public List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
        return tables;
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationtracker) {
        // [VanillaCopy] super, but remove call that checks that all vanilla tables are accounted for, because we aren't vanilla.
        map.forEach((id, builder) -> LootTableManager.func_227508_a_(validationtracker, id, builder));
    }
}
