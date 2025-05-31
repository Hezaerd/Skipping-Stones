package com.hezaerd.registry;

import com.hezaerd.entity.RockEntity;
import com.hezaerd.utils.ModLib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class ModEntityType {
    
    public static final EntityType<RockEntity> ROCK = register(
            "thrown_rock",
            EntityType.Builder.<RockEntity>create(RockEntity::new, SpawnGroup.MISC)
                    .dropsNothing()
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(32)
                    .trackingTickInterval(2)
    );
    
    public static void init() {
        ModLib.LOGGER.info("Successfully registered entities");
    }

    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }
    
    private static RegistryKey<EntityType<?>> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.ENTITY_TYPE, ModLib.of(id));
    }
    
    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return register(keyOf(id), type);
    }
}
