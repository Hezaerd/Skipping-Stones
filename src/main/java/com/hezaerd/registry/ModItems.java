package com.hezaerd.registry;

import com.hezaerd.item.RockItem;
import com.hezaerd.utils.ModLib;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;

public class ModItems {
    public static Item ROCK;
    
    public static void init() {
        ROCK = register("rock", RockItem::new, new Item.Settings().maxCount(64));
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((itemGroup -> {
                    itemGroup.add(ROCK);
                }));
        
        ModLib.LOGGER.info("Successfully registered items");
    }

    private static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, ModLib.of(name));
        Item item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }
}
