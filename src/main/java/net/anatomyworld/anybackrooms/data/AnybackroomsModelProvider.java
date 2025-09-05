package net.anatomyworld.anybackrooms.data;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.anatomyworld.anybackrooms.item.ModItems;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;

/**
 * Generates:
 *  - assets/anybackrooms/blockstates/lobby_wool.json
 *  - assets/anybackrooms/models/block/lobby_wool.json  (cube_all)
 *  - assets/anybackrooms/models/item/lobby_wool.json   (parent -> block model)
 *
 * Texture it expects: assets/anybackrooms/textures/block/lobby_wool.png
 */
public final class AnybackroomsModelProvider extends ModelProvider {
    public AnybackroomsModelProvider(PackOutput output) {
        super(output, "anybackrooms");
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Block: cube_all + blockstate + auto block-item client file
        blockModels.createTrivialCube(ModBlocks.LOBBY_WOOL.get());

        // Item: flat (item/generated). This creates BOTH:
        //  - assets/anybackrooms/items/almond_water.json  (definition)
        //  - assets/anybackrooms/models/item/almond_water.json (the baked model)
        itemModels.generateFlatItem(ModItems.ALMOND_WATER.get(), ModelTemplates.FLAT_ITEM);
    }
}
