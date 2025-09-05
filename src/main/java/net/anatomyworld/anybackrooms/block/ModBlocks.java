package net.anatomyworld.anybackrooms.block;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
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
import net.minecraft.world.item.BlockItem;

import java.util.Set;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnyBackroomsCore.MOD_ID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(AnyBackroomsCore.MOD_ID);

    /**
     * LOBBY_WOOL — texture like wool on all faces, but:
     *  - strength(-1, 3_600_000) -> unbreakable + bedrock-class blast resistance
     *  - not flammable
     *  - no drops
     *  - immovable by pistons
     */
    public static final DeferredBlock<Block> LOBBY_WOOL =
            BLOCKS.registerBlock("lobby_wool",
                    props -> new Block(props
                            .mapColor(MapColor.WOOL)
                            .sound(SoundType.WOOL)
                            .strength(-1.0F, 3_600_000.0F) //
                            .pushReaction(PushReaction.BLOCK) // pistons can’t move it
                            .noLootTable()                    // no drops
                    ));

    private static final Set<DeferredHolder<Block, ? extends Block>> SKIP_BLOCK_ITEMS = Set.of(
    );

    static {
        BLOCKS.getEntries().forEach(entry -> {
            if (!SKIP_BLOCK_ITEMS.contains(entry)) {
                // uses helper so Item.Properties has its id set
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
