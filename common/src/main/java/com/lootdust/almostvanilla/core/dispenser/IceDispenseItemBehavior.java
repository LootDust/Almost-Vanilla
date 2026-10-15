package com.lootdust.almostvanilla.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class IceDispenseItemBehavior extends OptionalDispenseItemBehavior {
    protected final double meltChance;

    public IceDispenseItemBehavior(double meltChance) {
        super();
        this.meltChance = meltChance;
    }

    @Override
    protected @NonNull ItemStack execute(BlockSource source, @NonNull ItemStack dispensed) {
        this.setSuccess(false);

        ServerLevel level = source.level();
        BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        BlockState targetState = level.getBlockState(targetPos);

        if (targetState.is(Blocks.WATER)) {
            this.setSuccess(true);
            level.setBlock(targetPos, Blocks.ICE.defaultBlockState(), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(Blocks.ICE.defaultBlockState()));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            if (level.getRandom().nextDouble() < meltChance) {
                dispensed.shrink(1);
            }
        }
        if (targetState.is(Blocks.SNOW_BLOCK)) {
            this.setSuccess(true);
            level.setBlock(targetPos, Blocks.POWDER_SNOW.defaultBlockState(), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(Blocks.POWDER_SNOW.defaultBlockState()));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            if (level.getRandom().nextDouble() < meltChance / 2 - 0.1) {
                dispensed.shrink(1);
            }
        }

        return dispensed;
    }
}
