package nl.dgoossens.chiselsandbits2.datagen;

import net.minecraft.block.Block;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.RegistryObject;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.common.registry.Registration;

import java.util.stream.Collectors;

public class BlockLootGenerator extends BlockLootTables {
    @Override
    protected void addTables() {
        Registration reg = ChiselsAndBits2.getInstance().getRegister();
        this.registerLootTable(reg.CHISELED_BLOCK.get(), new LootTable.Builder());
        this.registerDropSelfLootTable(reg.PREVIEW_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ChiselsAndBits2.getInstance().getRegister().getBlockRegister().getEntries().stream().map(RegistryObject::get).collect(Collectors.toSet());
    }
}
