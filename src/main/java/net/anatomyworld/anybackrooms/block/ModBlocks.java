package net.anatomyworld.anybackrooms.block;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnyBackroomsCore.MOD_ID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(AnyBackroomsCore.MOD_ID);

    /**
     * LOBBY_WOOL — wool-like, unbreakable, no drops, immovable.
     */
    public static final DeferredBlock<Block> LOBBY_WOOL =
            BLOCKS.registerBlock("lobby_wool",
                    props -> new Block(props
                            .mapColor(MapColor.WOOL)
                            .sound(SoundType.WOOL)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));

    /**
     * LOBBY_WALLPAPER — unbreakable, no drops, immovable.
     */
    public static final DeferredBlock<Block> LOBBY_WALLPAPER =
            BLOCKS.registerBlock("lobby_wallpaper",
                    props -> new Block(props
                            .mapColor(MapColor.WOOD)
                            .sound(SoundType.WOOD)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));

    public static final DeferredBlock<Block> LOBBY_WALLPAPER_PLINTH =
            BLOCKS.registerBlock("lobby_wallpaper_plinth",
                    props -> new Block(props
                            .mapColor(MapColor.WOOD)
                            .sound(SoundType.WOOD)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));

    public static final DeferredBlock<Block> LOBBY_WALLPAPER_OUTLET =
            BLOCKS.registerBlock("lobby_wallpaper_outlet",
                    props -> new Block(props
                            .mapColor(MapColor.WOOD)
                            .sound(SoundType.WOOD)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));

    /**
     * FLUORESCENT_LAMP — redstone lamp behavior, but unbreakable, no drops, immovable.
     * NOTE: Do NOT use ofFullCopy(...) here; keep the provided `props` so the ID stays set.
     */
    public static final DeferredBlock<RedstoneLampBlock> FLUORESCENT_LAMP =
            BLOCKS.registerBlock("fluorescent_lamp",
                    props -> new RedstoneLampBlock(
                            props
                                    // vanilla lamp light logic: lit -> 15, else 0
                                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)
                                    // your constraints
                                    .strength(-1.0F, 3_600_000.0F)
                                    .pushReaction(PushReaction.BLOCK)
                                    .noLootTable()
                                    // (optional cosmetics — comment out or tweak if you like)
                                    .mapColor(MapColor.NONE)
                                    .sound(SoundType.GLASS)
                    ));

    public static final DeferredBlock<Block> ACOUSTIC_TILE =
            BLOCKS.registerBlock("acoustic_tile",
                    props -> new Block(props
                            .mapColor(MapColor.WOOL)
                            .sound(SoundType.NETHER_WOOD)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));

    // The slab itself.
    public static final DeferredBlock<SlabBlock> ACOUSTIC_TILE_SLAB =
            BLOCKS.registerBlock("acoustic_tile_slab",
                    props -> new SlabBlock(props
                            .mapColor(MapColor.WOOL)
                            .sound(SoundType.NETHER_WOOD)
                            .strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()
                    ));


    /**
     * If you later add blocks that should NOT have items, put them in this set.
     */
    private static final Set<DeferredHolder<Block, ? extends Block>> SKIP_BLOCK_ITEMS = Set.of(
            // e.g. portal blocks, fluids, etc.
    );

    static {
        // Auto-register a BlockItem for every block unless skipped
        BLOCKS.getEntries().forEach(entry -> {
            if (!SKIP_BLOCK_ITEMS.contains(entry)) {
                DeferredItem<BlockItem> ignored = ITEMS.registerSimpleBlockItem(entry);
            }
        });
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private ModBlocks() {}
}
