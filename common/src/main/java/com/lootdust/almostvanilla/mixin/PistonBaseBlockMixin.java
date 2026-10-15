package com.lootdust.almostvanilla.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {
    @Redirect(method = "triggerEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void pistonBaseBlock$silenceIfWoolAdjacent(Level level, Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (sound != SoundEvents.PISTON_EXTEND && sound != SoundEvents.PISTON_CONTRACT) {
            level.playSound(except, pos, sound, source, volume, pitch);
            return;
        }
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(BlockStateProperties.FACING)) {
            level.playSound(except, pos, sound, source, volume, pitch);
            return;
        }

        Direction facing = state.getValue(BlockStateProperties.FACING);

        for (Direction direction : Direction.values()) {
            if (direction == facing) {
                continue;
            }

            if (level.getBlockState(pos.relative(direction)).is(BlockTags.WOOL)) {
                return;
            }
        }

        level.playSound(except, pos, sound, source, volume, pitch);
    }
}
