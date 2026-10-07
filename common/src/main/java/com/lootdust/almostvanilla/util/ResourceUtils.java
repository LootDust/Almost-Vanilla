package com.lootdust.almostvanilla.util;

import com.lootdust.almostvanilla.Constants;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class ResourceUtils {
    public static Identifier AnyIdentifier(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier AVIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    public static <T> ResourceKey<T> AVResourceKey(ResourceKey<? extends Registry<T>> registry, Identifier identifier) {
        return ResourceKey.create(registry, identifier);
    }
    public static <T> ResourceKey<T> AVResourceKey(ResourceKey<? extends Registry<T>> registry, String path) {
        return ResourceKey.create(registry, AVIdentifier(path));
    }
}
