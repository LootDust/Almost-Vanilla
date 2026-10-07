package com.lootdust.almostvanilla.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagUtils {
    public static <T> TagKey<T> getTagKey(ResourceKey<? extends Registry<T>> registry, Identifier identifier) {
        return TagKey.create(registry, identifier);
    }

    public static TagKey<Item> getItemTagKey(Identifier identifier) {
        return getTagKey(BuiltInRegistries.ITEM.key(), identifier);
    }
    public static TagKey<Item> getItemTagKey(String identifier) {
        return getTagKey(BuiltInRegistries.ITEM.key(), Identifier.parse(identifier));
    }
}
