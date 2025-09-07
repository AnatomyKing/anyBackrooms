// AnybackroomsModelProvider.java
package net.anatomyworld.anybackrooms.data;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.anatomyworld.anybackrooms.data.genmodels.TrivialBlock;
import net.anatomyworld.anybackrooms.data.genmodels.VanillaCopy;
import net.anatomyworld.anybackrooms.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public final class AnybackroomsModelProvider extends ModelProvider {

    public AnybackroomsModelProvider(PackOutput output) {
        super(output, AnyBackroomsCore.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Simple cubes you *do* want generated
        TrivialBlock.cubeAll(blockModels,
                ModBlocks.LOBBY_WOOL.get(),
                ModBlocks.ACOUSTIC_TILE.get()
        );

        // Columns
        TrivialBlock.columnAuto(blockModels, ModBlocks.LOBBY_WALLPAPER.get());

        // Lamp
        VanillaCopy.lampAuto(blockModels, ModBlocks.FLUORESCENT_LAMP.get());

        // Slab
        VanillaCopy.slabAuto(blockModels,
                ModBlocks.ACOUSTIC_TILE_SLAB.get(),
                ModBlocks.ACOUSTIC_TILE.get()
        );

        // Column overrides
        TrivialBlock.columnOverride(blockModels,
                ModBlocks.LOBBY_WALLPAPER_PLINTH.get(),
                TrivialBlock.texOf(ModBlocks.LOBBY_WALLPAPER_PLINTH.get()),
                TrivialBlock.texOf(ModBlocks.LOBBY_WALLPAPER.get(), "_top")
        );
        TrivialBlock.columnOverride(blockModels,
                ModBlocks.LOBBY_WALLPAPER_OUTLET.get(),
                TrivialBlock.texOf(ModBlocks.LOBBY_WALLPAPER_OUTLET.get()),
                TrivialBlock.texOf(ModBlocks.LOBBY_WALLPAPER.get(), "_top")
        );

        // === Drill piston blockstates ONLY (you provide models) ===
        // Both sticky & non-sticky use the same extended shell model: block/drill_piston_base.json
        VanillaCopy.drillPistonBaseStatesOnly(blockModels, ModBlocks.DRILL_PISTON.get(),        "drill_piston_base");
        VanillaCopy.drillPistonBaseStatesOnly(blockModels, ModBlocks.STICKY_DRILL_PISTON.get(), "drill_piston_base");
        VanillaCopy.drillPistonHeadStatesOnly(blockModels, ModBlocks.DRILL_PISTON_HEAD.get());

        // === Client items (point straight to block model) ===
        VanillaCopy.blockItemFromBlockModel(itemModels, ModBlocks.DRILL_PISTON.get());
        VanillaCopy.blockItemFromBlockModel(itemModels, ModBlocks.STICKY_DRILL_PISTON.get());

        // Flat item sample
        itemModels.generateFlatItem(ModItems.ALMOND_WATER.get(), ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return "Model Definitions - " + AnyBackroomsCore.MOD_ID;
    }
}
