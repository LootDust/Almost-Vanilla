package com.lootdust.almostvanilla.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.CommandResponseTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Locale;

public class HomeCommand {
    private static final CommandResponseTracker.MessagesWithArg<Entity, Vec3> RESPONSE_TELEPORT_TO_HOME = CommandResponseTracker.messages(
            (entity, var1, home) -> Component.translatable(
                    "commands.teleport.success.location.single", entity.getDisplayName(), formatDouble(home.x), formatDouble(home.y), formatDouble(home.z)
            ),
            (elementCount, var1, pos) -> Component.translatable(
                    "commands.teleport.success.location.multiple", elementCount, formatDouble(pos.x), formatDouble(pos.y), formatDouble(pos.z)
            )
    );

    private static final CommandResponseTracker.MessagesWithArg<Entity, Vec3> RESPONSE_TELEPORT_TO_WORLD_SPAWN = CommandResponseTracker.messages(
            (entity, var1, worldSpawn) -> Component.translatable(
                    "commands.teleport.success.location.single", entity.getDisplayName(), formatDouble(worldSpawn.x), formatDouble(worldSpawn.y), formatDouble(worldSpawn.z)
            ),
            (elementCount, var1, pos) -> Component.translatable(
                    "commands.teleport.success.location.multiple", elementCount, formatDouble(pos.x), formatDouble(pos.y), formatDouble(pos.z)
            )
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("home")
                        .executes(
                                context -> home(context.getSource())
                        )
                        .then(
                                Commands.literal("home")
                                        .executes(context -> home(context.getSource()))
                        )
                        .then(
                                Commands.literal("world_spawn")
                                        .executes(context -> worldSpawn(context.getSource()))
                        )
        );
    }

    private static int home(CommandSourceStack source) throws CommandSyntaxException {
        CommandResponseTracker<Entity> tracker = CommandResponseTracker.create();

        var self = source.getEntityOrException();
        if (self instanceof ServerPlayer player) {
            var respawnConfig = player.getRespawnConfig();
            if (respawnConfig != null) {
                var respawnLvl = source.getLevel().getServer().getLevel(respawnConfig.respawnData().dimension());
                if (respawnLvl != null) {
                    var respawnPos = respawnConfig.respawnData().pos();
                    var playerYRot = player.getYRot();
                    var playerXRot = player.getXRot();
                    player.teleportTo(respawnLvl, respawnPos.getX(), respawnPos.getY(), respawnPos.getZ(),
                            new HashSet<>(), playerYRot, playerXRot, false);

                    return tracker.sendFeedback(source, true, RESPONSE_TELEPORT_TO_HOME, new Vec3(respawnPos.getX(), respawnPos.getY(), respawnPos.getZ()));
                }
            }

            return worldSpawn(source);
        }

        return 0;
    }

    private static int worldSpawn(CommandSourceStack source) throws CommandSyntaxException {
        CommandResponseTracker<Entity> tracker = CommandResponseTracker.create();

        var self = source.getEntityOrException();
        if (self instanceof ServerPlayer player) {
            var respawnData = source.getLevel().getRespawnData();
            var worldRespawnLvl = source.getLevel().getServer().getLevel(respawnData.dimension());
            if (worldRespawnLvl != null) {
                var worldRespawnPos = respawnData.pos();
                var playerYRot = player.getYRot();
                var playerXRot = player.getXRot();
                player.teleportTo(worldRespawnLvl, worldRespawnPos.getX(), worldRespawnPos.getY(), worldRespawnPos.getZ(),
                        new HashSet<>(), playerYRot, playerXRot, false);

                return tracker.sendFeedback(source, true, RESPONSE_TELEPORT_TO_WORLD_SPAWN, new Vec3(worldRespawnPos.getX(), worldRespawnPos.getY(), worldRespawnPos.getZ()));
            }
        }

        return 0;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%f", value);
    }
}
