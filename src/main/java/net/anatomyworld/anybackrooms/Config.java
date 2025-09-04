package net.anatomyworld.anybackrooms;

import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * Zero-IO placeholder config.
 * - No ModConfigSpec
 * - No events
 * - Safe defaults that keep your existing references working
 */
public final class Config {
    private Config() {}

    public static final boolean logDirtBlock = false;
    public static final int     magicNumber  = 42;
    public static final String  magicNumberIntroduction = "The magic number is... ";
    public static final Set<Item> items = Set.of();
}
