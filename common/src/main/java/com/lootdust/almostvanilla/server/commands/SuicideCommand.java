package com.lootdust.almostvanilla.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.CommandResponseTracker;
import net.minecraft.world.entity.Entity;

public class SuicideCommand {
    private static final CommandResponseTracker.Messages<Entity> RESPONSE_SUICIDE = CommandResponseTracker.messages(
            (entity, var1) -> Component.translatable("commands.kill.success.single", entity.getDisplayName()),
            (entityCount, var1) -> Component.translatable("commands.kill.success.multiple", entityCount)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("suicide")
                        .executes(context -> suicide(context.getSource()))
        );
    }

    private static int suicide(CommandSourceStack source) throws CommandSyntaxException {
        CommandResponseTracker<Entity> tracker = CommandResponseTracker.create();

        var self = source.getEntityOrException();
        self.kill(source.getLevel());
        tracker.track(self);

        return tracker.sendFeedback(source, true, RESPONSE_SUICIDE);
    }
}
