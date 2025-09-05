package net.anatomyworld.anybackrooms;

import com.mojang.logging.LogUtils;
import net.anatomyworld.anybackrooms.world.BackroomsChunkFiller;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

// registries + client/datagen
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.anatomyworld.anybackrooms.item.ModItems;
import net.anatomyworld.anybackrooms.item.ModCreativeTabs;
import net.anatomyworld.anybackrooms.data.ModDataGenerators;
import net.anatomyworld.anybackrooms.client.render.AnybackroomsRenderLayers;

@Mod(AnyBackroomsCore.MOD_ID)
public final class AnyBackroomsCore {
    public static final String MOD_ID = "anybackrooms";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Dimension key: anybackrooms:anybackrooms */
    public static final ResourceKey<Level> PXN_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "anybackrooms"));

    public AnyBackroomsCore(IEventBus modBus, ModContainer container) {
        LOGGER.info("[{}] core loaded", MOD_ID);

        // --- registry wires ---
        ModBlocks.register(modBus);
        ModItems.register(modBus);       // <- you already call this; keep it
        ModCreativeTabs.register(modBus);

        // --- datagen providers (client/server providers can be added here) ---
        modBus.addListener(this::commonSetup);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        modBus.addListener(ModDataGenerators::gatherData);

        // --- client-only render layer shim (safe even if currently empty) ---
        if (FMLLoader.getDist().isClient()) {
            modBus.addListener(AnybackroomsRenderLayers::onModifyBakingResult);
        }

        modBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent e) {
        // common setup if needed
    }

    private void commonSetup(final FMLCommonSetupEvent e) {
        // shared init if needed
    }

    /** Handles chunk population on the (logical) server in both dedicated + singleplayer. */
    @EventBusSubscriber(modid = MOD_ID)
    public static final class ServerEvents {
        @SubscribeEvent
        public static void onChunkLoad(final ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            if (!level.dimension().equals(PXN_DIMENSION)) return;

            level.getServer().execute(() -> {
                if (event.getChunk() instanceof LevelChunk chunk) {
                    BackroomsChunkFiller.fill(level, chunk);
                }
            });
        }
    }
}
