package net.anatomyworld.anybackrooms.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Writes assets/anybackrooms/lang/en_us.json */
public final class AnybackroomsLanguageProvider extends LanguageProvider {
    private static final String MODID = "anybackrooms"; // decouple from core class

    public AnybackroomsLanguageProvider(PackOutput output, String locale) {
        super(output, MODID, locale);
    }

    @Override
    protected void addTranslations() {
        add("block.anybackrooms.lobby_wool", "Lobby Wool");
        add("itemGroup.anybackrooms.anybackrooms_tab", "AnyBackrooms");
    }
}
