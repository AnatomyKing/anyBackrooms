package net.anatomyworld.anybackrooms.block;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/** All custom blocks + auto BlockItems (1.21.8-safe). */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AnyBackroomsCore.MOD_ID);
    public static final DeferredRegister.Items  ITEMS  =
            DeferredRegister.createItems(AnyBackroomsCore.MOD_ID);

    /* -------------------- Example Blocks (optional) -------------------- */
    // Add your blocks here. Example placeholder:
    public static final DeferredBlock<Block> STAINED_PANEL =
            BLOCKS.registerBlock("stained_panel",
                    props -> new Block(props
                            .mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
                            .pushReaction(PushReaction.NORMAL)));

    /* -------------------- Auto BlockItems -------------------- */

    // Skip BlockItems for blocks that shouldn't have one
    private static final Set<DeferredHolder<Block, ? extends Block>> SKIP_BLOCK_ITEMS = Set.of(
            // e.g. portal or fire-like blocks:
            // SOME_FIRE_BLOCK, SOME_PORTAL_BLOCK
    );

    static {
        // For every registered block, create a matching BlockItem unless skipped.
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
