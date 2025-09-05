package net.anatomyworld.anybackrooms.item;

import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AnyBackroomsCore.MOD_ID);

    // --- Example simple item so the tab has an icon right away ---
    public static final DeferredItem<Item> ALMOND_WATER =
            ITEMS.registerItem("almond_water", props -> new Item(props.stacksTo(16).rarity(Rarity.COMMON)));

    // Add more items here, e.g.:
    // public static final DeferredItem<Item> DULL_LIGHT =
    //         ITEMS.registerItem("dull_light", props -> new Item(props));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
