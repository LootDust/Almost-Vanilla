package com.lootdust.almostvanilla.event;

import com.lootdust.almostvanilla.Constants;
import com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior;
import com.lootdust.almostvanilla.server.commands.HomeCommand;
import com.lootdust.almostvanilla.server.commands.SuicideCommand;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoEvents {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ModDispenseItemBehavior.registerDispenseItemBehaviors(BuiltInRegistries.ITEM, serverLevel ->
                FakePlayerFactory.get(serverLevel, new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]")));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HomeCommand.register(event.getDispatcher());
        SuicideCommand.register(event.getDispatcher());
    }
}
