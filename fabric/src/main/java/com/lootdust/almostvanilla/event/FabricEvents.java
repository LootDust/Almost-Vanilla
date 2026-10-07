package com.lootdust.almostvanilla.event;

import com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;

public class FabricEvents {
    public static void bootStrap() {
        ServerLifecycleEvents.SERVER_STARTED.register((_) -> ModDispenseItemBehavior.registerDispenseItemBehaviors(BuiltInRegistries.ITEM));
    }
}
