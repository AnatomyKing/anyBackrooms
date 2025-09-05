package net.anatomyworld.anybackrooms.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
        super(out,
                Set.of(),
                List.of(new SubProviderEntry(ModBlockLootSubProvider::new, LootContextParamSets.BLOCK)),
                lookup);
    }
}
