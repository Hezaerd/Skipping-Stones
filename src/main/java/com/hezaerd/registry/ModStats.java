package com.hezaerd.registry;

import com.hezaerd.utils.ModLib;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

public class ModStats {
    public static final Identifier BEST_ROCK_SKIPS = register("best_rock_skips", StatFormatter.DEFAULT);
    public static final Identifier BEST_SKIP_DISTANCE = register("best_skip_distance", StatFormatter.DISTANCE);
    
    public static void init() {
        ModLib.LOGGER.info("Successfully registered stats");
    }
    
    private static Identifier register(String id, StatFormatter formatter) {
        Identifier identifier = ModLib.of(id);
        Registry.register(Registries.CUSTOM_STAT, id, identifier);
        Stats.CUSTOM.getOrCreateStat(identifier, formatter);
        return identifier;
    }
}
