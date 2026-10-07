package com.lootdust.almostvanilla.core.dispenser;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;

import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
import static net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS;

public interface ModDispenseItemBehavior
{
    static void registerDispenseItemBehaviors(HolderGetter<Item> items) {
        ToolDispenseBehaviourRegistry(items, ItemTags.AXES, BlockTags.MINEABLE_WITH_AXE);
        ToolDispenseBehaviourRegistry(items, ItemTags.PICKAXES, BlockTags.MINEABLE_WITH_PICKAXE);
        ToolDispenseBehaviourRegistry(items, ItemTags.SHOVELS, BlockTags.MINEABLE_WITH_SHOVEL);
        ToolDispenseBehaviourRegistry(items, ItemTags.HOES, BlockTags.MINEABLE_WITH_HOE);

        SaplingDispenseBehaviourRegistry(items);
    }

    private static void ToolDispenseBehaviourRegistry(HolderGetter<Item> items, TagKey<Item> toolTag, TagKey<Block> blockTag) {
        for (Holder<Item> holder : items.getOrThrow(toolTag)) {
            DispenserBlock.registerBehavior(holder.value(), new OptionalDispenseItemBehavior() {
                @Override
                protected @NonNull ItemStack execute(@NonNull BlockSource source, @NonNull ItemStack dispensed) {
                    this.setSuccess(false);
                    ServerLevel level = source.level();
                    BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                    BlockState state = level.getBlockState(pos);
                    if (state.is(blockTag)) {
                        this.setSuccess(true);
                        if (!level.isClientSide()) {
                            level.destroyBlock(pos, false, null);
                            level.gameEvent(null, GameEvent.BLOCK_DESTROY, pos);
                            if (dispensed.isCorrectToolForDrops(state)) {
                                for (ItemStack stack : Block.getDrops(state, level, pos, source.blockEntity(), null, dispensed)) {
                                    ItemStack remains = source.blockEntity().insertItem(stack);
                                    if (!remains.isEmpty()) {
                                        level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), remains));
                                    }
                                }
                            }
                            if (!dispensed.getComponents().has(DataComponents.UNBREAKABLE)) {
                                dispensed.hurtAndBreak(1, level, null, _ -> {
                                });
                            }
                        }
                    }
                    return dispensed;
                }
            });
        }
    }

    private static void SaplingDispenseBehaviourRegistry(HolderGetter<Item> items) {
        for (Holder<Item> holder : items.getOrThrow(ItemTags.SAPLINGS)) {
            DispenserBlock.registerBehavior(holder.value(), new OptionalDispenseItemBehavior() {
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
            });
        }
    }
}
