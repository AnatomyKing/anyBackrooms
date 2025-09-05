package net.anatomyworld.anybackrooms.data;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, AnyBackroomsCore.MOD_ID);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        /* ---------- your existing vanilla tags ---------- */

        // tools
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(

        );
        tag(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL).add(

        );
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE).add(

        );
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE).add(

        );

        // leaves/logs/planks
        tag(net.minecraft.tags.BlockTags.LEAVES).add(

        );
        tag(net.minecraft.tags.BlockTags.LOGS).add(

        );
        tag(net.minecraft.tags.BlockTags.LOGS_THAT_BURN).add(

        );
        tag(net.minecraft.tags.BlockTags.PLANKS).add(

        );

        // crops / saplings
        tag(net.minecraft.tags.BlockTags.CROPS).add(

        );
        tag(net.minecraft.tags.BlockTags.SAPLINGS).add(

        );

    }
}
