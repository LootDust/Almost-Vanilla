package com.lootdust.almostvanilla.core.dispenser;

import com.google.common.collect.ImmutableMap;
import com.lootdust.almostvanilla.util.TagUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class MaceDispenseItemBehavior extends OptionalDispenseItemBehavior {
    private static final TagKey<Block> COBBLESTONES_TAG = TagUtils.getTagKey(Registries.BLOCK, Identifier.parse("c:cobblestones"));

    @Override
    protected @NonNull ItemStack execute(BlockSource source, @NonNull ItemStack dispensed) {
        this.setSuccess(false);

        ServerLevel level = source.level();
        BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        BlockState targetState = level.getBlockState(targetPos);

        if (
                // Simple cobblestones transforms
                simpleBlockTransform(level, targetPos, targetState, COBBLESTONES_TAG, Blocks.GRAVEL) ||
                simpleBlockTransform(level, targetPos, targetState, Blocks.GRAVEL, Blocks.SAND) ||
                // Simple stones transforms and drops
                simpleBlockTransform(level, targetPos, targetState, Blocks.GRANITE, Blocks.RED_SANDSTONE) ||
                simpleBlockTransform(level, targetPos, targetState, Blocks.RED_SANDSTONE, Blocks.RED_SAND) ||
                simpleBlockTransform(level, targetPos, targetState, Blocks.ANDESITE, Blocks.TUFF) ||
                simpleBlockTransform(level, targetPos, targetState, Blocks.DIORITE, Blocks.CALCITE) ||
                simpleChancedBlockDrops(level, targetPos, targetState, Blocks.CALCITE, ImmutableMap.of(
                        new ItemStackTemplate(Items.QUARTZ, 2), 1.0,
                        new ItemStackTemplate(Items.QUARTZ), 0.5
                )) ||
                // Special simple transforms
                simpleBlockTransform(level, targetPos, targetState, Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST) ||
                sameBlockTransform(level, targetPos, targetState, Blocks.DAMAGED_ANVIL, Blocks.CHIPPED_ANVIL) ||
                sameBlockTransform(level, targetPos, targetState, Blocks.CHIPPED_ANVIL, Blocks.ANVIL) ||
                petrifiedOakSlabTransform(level, targetPos, targetState) ||
                // Special block transforms and drops
                simpleSingleChancedBlockTransform(level, targetPos, targetState, Blocks.BASALT, ImmutableMap.of(
                        Blocks.DEEPSLATE, 0.5,
                        Blocks.BLACKSTONE, 0.9,
                        Blocks.GILDED_BLACKSTONE, 1.0
                )) ||
                simpleChancedBlockDrops(level, targetPos, targetState, Blocks.NETHERRACK, ImmutableMap.of(
                        new ItemStackTemplate(Items.NETHERITE_SCRAP, 1), 0.015625
                )) ||
                tuffTransformAndDrops(level, targetPos, targetState) ||
                gildedBlackstoneTransformAndDrops(level, targetPos, targetState)) {
            this.setSuccess(true);
        }
        return dispensed;
    }

    private static boolean simpleBlockTransform(ServerLevel level, BlockPos targetPos, BlockState targetState, TagKey<Block> targetBlockTag, Block transformedBlock) {
        if (targetState.is(targetBlockTag)) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(targetPos, transformedBlock.defaultBlockState(), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            return true;
        }

        return false;
    }
    private static boolean simpleBlockTransform(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, Block transformedBlock) {
        if (targetState.is(targetBlock)) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(targetPos, transformedBlock.defaultBlockState(), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            return true;
        }

        return false;
    }
    private static boolean sameBlockTransform(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, Block transformedBlock) {
        if (targetState.is(targetBlock)) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(targetPos, transformedBlock.withPropertiesOf(targetState), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            return true;
        }

        return false;
    }

    private static boolean simpleChancedBlockTransform(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, Map<Block, Double> transforms) {
        for (var entry : transforms.entrySet()) {
            if (level.getRandom().nextDouble() < entry.getValue()) {
                return simpleBlockTransform(level, targetPos, targetState, targetBlock, entry.getKey());
            }
        }

        return false;
    }
    private static boolean simpleSingleChancedBlockTransform(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, Map<Block, Double> transforms) {
        double chosen = level.getRandom().nextDouble();
        for (var entry : transforms.entrySet()) {
            if (chosen < entry.getValue()) {
                return simpleBlockTransform(level, targetPos, targetState, targetBlock, entry.getKey());
            }
        }

        return false;
    }

    private static boolean simpleBlockDrops(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, ItemStackTemplate... drops) {
        if (targetState.is(targetBlock)) {
            level.destroyBlock(targetPos, false);
            for (ItemStackTemplate drop : drops) {
                level.addFreshEntity(new ItemEntity(level,
                        targetPos.getX() + 0.5d, targetPos.getY() + 0.25d, targetPos.getZ() + 0.5d,
                        drop.create()));
            }
            return true;
        }

        return false;
    }

    private static boolean gildedBlackstoneTransformAndDrops(ServerLevel level, BlockPos targetPos, BlockState targetState) {
        if (targetState.is(Blocks.GILDED_BLACKSTONE)) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(targetPos, Blocks.BLACKSTONE.defaultBlockState(), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            level.addFreshEntity(new ItemEntity(level,
                    targetPos.getX() + 0.5d, targetPos.getY() + 0.25d, targetPos.getZ() + 0.5d,
                    new ItemStack(Items.GOLD_NUGGET, level.getRandom().nextFloat() < 0.5 ? 5 : 4)));
            return true;
        }

        return false;
    }

    private static boolean simpleChancedBlockDrops(ServerLevel level, BlockPos targetPos, BlockState targetState, Block targetBlock, Map<ItemStackTemplate, Double> drops) {
        if (targetState.is(targetBlock)) {
            level.destroyBlock(targetPos, false);
            drops.forEach((drop, chance) -> {
                if (level.getRandom().nextDouble() < chance) level.addFreshEntity(new ItemEntity(level,
                            targetPos.getX() + 0.5d, targetPos.getY() + 0.25d, targetPos.getZ() + 0.5d,
                            drop.create()));
            });
            return true;
        }

        return false;
    }

    private static boolean tuffTransformAndDrops(ServerLevel level, BlockPos targetPos, BlockState targetState) {
        if (targetState.is(Blocks.TUFF)) {
            float resultIndex = level.getRandom().nextFloat();
            if (resultIndex < 0.4) {
                level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
                level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(targetPos, Blocks.GRAVEL.defaultBlockState(), UPDATE_ALL);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            } else if (resultIndex < 0.7) {
                level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
                level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(targetPos, Blocks.BLACKSTONE.defaultBlockState(), UPDATE_ALL);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            } else if (resultIndex < 0.8) {
                level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
                level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(targetPos, Blocks.GILDED_BLACKSTONE.defaultBlockState(), UPDATE_ALL);
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            } else if (resultIndex < 0.9) {
                level.destroyBlock(targetPos, false);
                level.addFreshEntity(new ItemEntity(level, targetPos.getX() + 0.5d, targetPos.getY() + 0.25d, targetPos.getZ() + 0.5d,
                        new ItemStack(Items.LAPIS_LAZULI, level.getRandom().nextFloat() < 0.5 ? 3 : 2)));
            } else {
                level.destroyBlock(targetPos, false);
                level.addFreshEntity(new ItemEntity(level, targetPos.getX() + 0.5d, targetPos.getY() + 0.25d, targetPos.getZ() + 0.5d,
                        new ItemStack(Items.REDSTONE, level.getRandom().nextFloat() < 0.5 ? 5 : 3)));
            }
            return true;
        }

        return false;
    }

    private static boolean petrifiedOakSlabTransform(ServerLevel level, BlockPos targetPos, BlockState targetState) {
        if (targetState.is(Blocks.OAK_SLAB)) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, targetPos, Block.getId(targetState));
            level.playSound(null, targetPos, targetState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(targetPos, Blocks.PETRIFIED_OAK_SLAB.withPropertiesOf(targetState), UPDATE_ALL);
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
            return true;
        }

        return false;
    }
}
