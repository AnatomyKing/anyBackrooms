package net.anatomyworld.anybackrooms;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Basic empty config for HarambeCore.
 * You can add options later by adding entries to the SPEC builder.
 */
public final class Config {
    public static final ModConfigSpec COMMON_SPEC;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // Example placeholder option (disabled by default, does nothing yet)
        builder.push("general");
        builder.comment("Example setting you can toggle in the future");
        builder.define("exampleOption", false);
        builder.pop();

        COMMON_SPEC = builder.build();
    }
}
