package com.lootdust.almostvanilla.event;

import com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior;
import com.lootdust.almostvanilla.server.commands.HomeCommand;
import com.lootdust.almostvanilla.server.commands.SuicideCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;

public class FabricEvents {
    public static void bootStrap() {
        ServerLifecycleEvents.SERVER_STARTED.register((_) -> {
            ModDispenseItemBehavior.registerDispenseItemBehaviors(BuiltInRegistries.ITEM, FakePlayer::get);
        });

        CommandRegistrationCallback.EVENT.register(((dispatcher, buildContext, selection) -> {
            HomeCommand.register(dispatcher);
            SuicideCommand.register(dispatcher);
        }));
    }
}
