package com.lootdust.almostvanilla.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;

import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
import static net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS;

public class SaplingDispenseItemBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected @NonNull ItemStack execute(@NonNull BlockSource source, @NonNull ItemStack dispensed) {
        this.setSuccess(false);
        if (dispensed.getItem() instanceof BlockItem blockItem) {
            ServerLevel level = source.level();
            BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            BlockState state = level.getBlockState(pos);
            if (state == Blocks.AIR.defaultBlockState() || state.is(BlockTags.REPLACEABLE_BY_TREES)) {
                BlockState downState = level.getBlockState(pos.relative(Direction.DOWN));
                if (downState.is(BlockTags.SUPPORTS_VEGETATION)) {
                    if (!level.isClientSide()) {
                        level.setBlock(pos, blockItem.getBlock().defaultBlockState(), UPDATE_NEIGHBORS | UPDATE_CLIENTS);
                        level.gameEvent(null, GameEvent.BLOCK_PLACE, pos);
                    }
                    dispensed.shrink(1);
                    this.setSuccess(true);
                }
            } else if (state.is(BlockTags.SUPPORTS_VEGETATION)) {
                BlockPos relativePos = pos.relative(Direction.UP);
                BlockState upState = level.getBlockState(relativePos);
                if (upState == Blocks.AIR.defaultBlockState() || upState.is(BlockTags.REPLACEABLE_BY_TREES)) {
                    if (!level.isClientSide()) {
                        level.setBlock(relativePos, blockItem.getBlock().defaultBlockState(), UPDATE_NEIGHBORS | UPDATE_CLIENTS);
                        level.gameEvent(null, GameEvent.BLOCK_PLACE, relativePos);
                    }
                    dispensed.shrink(1);
                    this.setSuccess(true);
                }
            }
        }
        return dispensed;
    }
}
