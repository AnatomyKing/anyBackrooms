package net.anatomyworld.anybackrooms.item;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnyBackroomsCore.MOD_ID);

    // Skip registry paths you don’t want visible in the tab (optional)
    private static final Set<String> BLACKLIST = Set.of(
            // "some_hidden_item"
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANYBACKROOMS_TAB =
            CREATIVE_TABS.register("anybackrooms_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AnyBackroomsCore.MOD_ID + ".anybackrooms_tab"))
                    .icon(() -> new ItemStack(ModItems.ALMOND_WATER.get()))
                    .displayItems((params, output) -> {
                        List<Item> mine = new ArrayList<>();
                        for (Item item : BuiltInRegistries.ITEM) {
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                            if (id != null
                                    && AnyBackroomsCore.MOD_ID.equals(id.getNamespace())
                                    && !BLACKLIST.contains(id.getPath())) {
                                mine.add(item);
                            }
                        }
                        // Stable order by registry path
                        mine.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).getPath()));
                        mine.forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
