package com.lootdust.almostvanilla.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class AxeDispenseItemBehavior extends OptionalDispenseItemBehavior
{
    private static final int MAX_LOGS = 256;
    private static final int MAX_LEAVES = 1024;

    @Override
    protected @NonNull ItemStack execute(BlockSource source, @NonNull ItemStack dispensed) {
        ServerLevel level = source.level();
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos targetPos = source.pos().relative(facing);
        BlockState targetState = level.getBlockState(targetPos);

        if (isLog(targetState)) {
            this.setSuccess(destroyTree(level, targetPos, dispensed));
        } else {
            if (targetState.is(BlockTags.MINEABLE_WITH_AXE)) {
                this.setSuccess(true);
                if (!level.isClientSide()) {
                    level.destroyBlock(targetPos, false, null);
                    if (dispensed.isCorrectToolForDrops(targetState)) {
                        var drops = Block.getDrops(targetState, level, targetPos, source.blockEntity(), null, dispensed);
                        ModDispenseItemBehavior.insertOrDrop(level, targetPos, source.blockEntity(), drops);
                    }
                    ModDispenseItemBehavior.damageTool(dispensed, level);
                }
            }
        }

        return dispensed;
    }

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM);
    }

    public static boolean isLeaf(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty
                    && property.getName().equals("distance")
                    && property != BlockStateProperties.STABILITY_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private static int getLeafDistance(BlockState state) {
        IntegerProperty distance = LeavesBlock.DISTANCE;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty ip && property.getName().equals("distance")) {
                distance = ip;
            }
        }
        return state.getValue(distance);
    }

    private static int nonDecayingLeafDistance(BlockState state) {
        if (state.is(Blocks.RED_MUSHROOM_BLOCK)) return 2;
        if (state.is(Blocks.BROWN_MUSHROOM_BLOCK)) return 3;
        if (state.is(BlockTags.WART_BLOCKS)) return 3;
        return -1;
    }

    public static boolean destroyTree(Level level, BlockPos startPos, ItemStack tool) {
        if (level.isClientSide()) return false;

        //if (!validateCut(level, startPos)) return;

        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(startPos);

        while (!frontier.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos current = frontier.poll();
            if (!visited.add(current)) continue;
            if (!isLog(level.getBlockState(current))) continue;

            logs.add(current);
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!visited.contains(next)) frontier.add(next);
            }
        }
        if (logs.size() >= MAX_LOGS) return false;

        Set<BlockPos> leaves = new HashSet<>();
        visited.clear();
        visited.addAll(logs);
        frontier.clear();
        frontier.addAll(logs);

        while (!frontier.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos prev = frontier.poll();
            BlockState prevState = level.getBlockState(prev);
            int prevLeafDistance = isLeaf(prevState) ? getLeafDistance(prevState) : 0;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos current = prev.offset(dx, dy, dz);
                        if (visited.contains(current)) continue;

                        BlockState state = level.getBlockState(current);
                        BlockPos delta = current.subtract(startPos);
                        int horizontalDistance = Math.max(
                                Math.abs(delta.getX()), Math.abs(delta.getZ()));

                        if (horizontalDistance <= nonDecayingLeafDistance(state)) {
                            leaves.add(current);
                            visited.add(current);
                            frontier.add(current);
                        } else if (isLeaf(state) && getLeafDistance(state) > prevLeafDistance) {
                            leaves.add(current);
                            visited.add(current);
                            frontier.add(current);
                        }
                    }
                }
            }
        }

        List<ItemStack> allDrops = new ArrayList<>();

        for (BlockPos pos : leaves) {
            BlockState state = level.getBlockState(pos);
            if (isLeaf(state) || nonDecayingLeafDistance(state) >= 0) {
                allDrops.addAll(Block.getDrops(state, (ServerLevel) level, pos, null, null, tool));
                level.destroyBlock(pos, false);
            }
        }
        for (BlockPos pos : logs) {
            BlockState state = level.getBlockState(pos);
            if (isLog(level.getBlockState(pos))) {
                allDrops.addAll(Block.getDrops(state, (ServerLevel) level, pos, null, null, tool));
                level.destroyBlock(pos, false);
            }
        }

        for (ItemStack drop : allDrops) {
            level.addFreshEntity(new ItemEntity(level,
                    startPos.getX() + 0.5d, startPos.getY() + 0.25d, startPos.getZ() + 0.5d, drop));
        }

        ModDispenseItemBehavior.damageTool(tool, (ServerLevel) level);

        return true;
    }

    private static boolean validateCut(BlockGetter reader, BlockPos pos) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(pos);
        frontier.add(pos.above());
        int posY = pos.getY();

        while (!frontier.isEmpty()) {
            BlockPos current = frontier.poll();
            if (!visited.add(current)) continue;

            BlockPos below = current.below();
            boolean lowerLayer = current.getY() == posY;
            BlockState currentState = reader.getBlockState(current);
            BlockState belowState = reader.getBlockState(below);

            if (!isLog(currentState)) continue;
            if (!lowerLayer && !pos.equals(below) && isLog(belowState)) return false;

            for (Direction dir : Direction.values()) {
                if (dir == Direction.DOWN) continue;
                if (dir == Direction.UP && !lowerLayer) continue;
                BlockPos next = current.relative(dir);
                if (!visited.contains(next)) frontier.add(next);
            }
        }
        return true;
    }
}
