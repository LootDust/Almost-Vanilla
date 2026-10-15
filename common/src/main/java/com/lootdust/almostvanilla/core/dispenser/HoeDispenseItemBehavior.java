package com.lootdust.almostvanilla.core.dispenser;

import com.lootdust.almostvanilla.mixin.CropBlockMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

import static com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior.damageTool;
import static com.lootdust.almostvanilla.core.dispenser.ModDispenseItemBehavior.insertOrDrop;

public class HoeDispenseItemBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource source, ItemStack dispensed) {
        this.setSuccess(false);

        ServerLevel level = source.level();
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        BlockState state = level.getBlockState(pos);

        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            this.setSuccess(true);
            if (level.isClientSide()) return dispensed;

            level.destroyBlock(pos, false, null);
            level.gameEvent(null, GameEvent.BLOCK_DESTROY, pos);

            if (dispensed.isCorrectToolForDrops(state)) {
                List<ItemStack> drops = Block.getDrops(state, level, pos,
                        source.blockEntity(), null, dispensed);
                insertOrDrop(level, pos, source.blockEntity(), drops);
            }
            damageTool(dispensed, level);

        } else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            this.setSuccess(true);
            if (level.isClientSide()) return dispensed;

            Item seed = ((CropBlockMixin) crop).almostvanilla$invokeGetBaseSeedId().asItem();

            List<ItemStack> drops = Block.getDrops(state, level, pos, source.blockEntity(), null, dispensed);

            boolean canReplant = false;

            for (ItemStack drop : drops) {
                if (!drop.isEmpty() && drop.is(seed)) {
                    drop.shrink(1);
                    canReplant = true;
                    break;
                }
            }

            if (canReplant) {
                level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
            } else {
                level.removeBlock(pos, false);
                level.gameEvent(null, GameEvent.BLOCK_DESTROY, pos);
            }

            insertOrDrop(level, pos, source.blockEntity(), drops);
            damageTool(dispensed, level);
        }

        return dispensed;
    }
}
