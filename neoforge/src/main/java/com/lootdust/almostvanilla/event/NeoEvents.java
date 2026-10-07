package com.lootdust.almostvanilla.event;

import com.lootdust.almostvanilla.Constants;
import com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoEvents {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ModDispenseItemBehavior.registerDispenseItemBehaviors(BuiltInRegistries.ITEM);
    }
}
