package net.anatomyworld.anybackrooms.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModDataGenerators {

    /**
     * Register ALL providers for the "clientData" run here (NeoForge 1.21.6–1.21.8 style).
     * IMPORTANT: This method must be registered exactly once (see HarambeCore constructor).
     */
    public static void gatherData(final GatherDataEvent.Client event) {
        // If you ever split client/server providers, you can also use GatherDataEvent.Server.
        // The MDK defaults to putting everything in Client. :contentReference[oaicite:1]{index=1}

        // Recipes (Runner pattern in 1.21.x)
        event.createProvider(ModRecipeProvider.Runner::new);

        // Block tags
        event.createProvider(ModBlockTagsProvider::new);

        // Loot tables
        event.createProvider(ModLootTableProvider::new);

        // NEW: models (blockstates + models + auto item models)
        event.createProvider(AnybackroomsModelProvider::new);

        // NEW: lang (en_us)
        event.createProvider(out -> new AnybackroomsLanguageProvider(out, "en_us"));

        // If/when you add item tags that depend on block tags, use:
        // event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
    }

    private ModDataGenerators() {}
}
