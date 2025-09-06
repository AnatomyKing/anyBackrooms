package net.anatomyworld.anybackrooms.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Auto language provider (en_us or any locale you pass).
 * - Adds names for ALL blocks in "anybackrooms".
 * - Adds names for NON-block items only (skips BlockItems; they use block.* keys).
 * - Humanizes ids: "lobby_wallpaper_plinth" -> "Lobby Wallpaper Plinth".
 * - OVERRIDES (full keys) applied last and win.
 *
 * Output: assets/anybackrooms/lang/<locale>.json
 */
public final class AnybackroomsLanguageProvider extends LanguageProvider {
    private static final String MODID = "anybackrooms";

    /** Full translation key -> custom text (wins last). */
    private static final Map<String, String> OVERRIDES = new LinkedHashMap<>(Map.of(
            // "block.anybackrooms.lobby_wallpaper_plinth", "Lobby Plinth (Wallpaper Top)",
            // "item.anybackrooms.almond_water", "Almond Water",
            // "itemGroup.anybackrooms.anybackrooms_tab", "AnyBackrooms"
    ));

    public AnybackroomsLanguageProvider(PackOutput output, String locale) {
        super(output, MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // 1) Blocks -> block.anybackrooms.<id> = Human Name
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (id == null || !MODID.equals(id.getNamespace())) continue;
            String path = id.getPath();
            add("block." + MODID + "." + path, humanize(path));
        }

        // 2) Items that are NOT BlockItems -> item.anybackrooms.<id> = Human Name
        for (Item it : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
            if (id == null || !MODID.equals(id.getNamespace())) continue;
            if (it instanceof BlockItem) continue; // redundant; BlockItem uses the block.* key by default
            String path = id.getPath();
            add("item." + MODID + "." + path, humanize(path));
        }

        // 3) Creative tab (overridable below)
        String tabKey = "itemGroup." + MODID + ".anybackrooms_tab";
        add(tabKey, OVERRIDES.getOrDefault(tabKey, "AnyBackrooms"));

        // 4) Explicit overrides (win last). Note: overriding item.* for BlockItems
        // won't change the name unless you also override the item's description id.
        OVERRIDES.forEach(this::add);
    }

    /** "lobby_wallpaper_plinth" -> "Lobby Wallpaper Plinth" (with a few acronym touch-ups). */
    private static String humanize(String registryPath) {
        String[] parts = registryPath.toLowerCase(Locale.ROOT).split("[_\\-]+");
        StringBuilder out = new StringBuilder(parts.length * 6);
        for (String p : parts) {
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.length() > 1 ? p.substring(1) : "")
                    .append(' ');
        }
        String s = out.toString().trim();
        // optional acronym touch-ups
        s = s.replace("Tnt", "TNT").replace("Tv", "TV").replace("Gps", "GPS");
        return s;
    }
}
