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
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class AnybackroomsModelProvider extends ModelProvider {

    public AnybackroomsModelProvider(PackOutput output) {
        super(output, AnyBackroomsCore.MOD_ID);
    }


    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Simple cubes
        TrivialBlock.cubeAll(blockModels,
                ModBlocks.LOBBY_WOOL.get(),
                ModBlocks.ACOUSTIC_TILE.get()       // <-- add this so the full block has a blockstate/model
        );

        // Columns
        TrivialBlock.columnAuto(blockModels, ModBlocks.LOBBY_WALLPAPER.get());

        // Lamp (with the earlier path fix)
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

        // Items
        itemModels.generateFlatItem(ModItems.ALMOND_WATER.get(), ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return "Model Definitions - " + AnyBackroomsCore.MOD_ID;
    }
}
