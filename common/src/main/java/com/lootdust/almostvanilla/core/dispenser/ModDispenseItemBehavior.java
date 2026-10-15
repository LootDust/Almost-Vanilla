package com.lootdust.almostvanilla.core.dispenser;

import com.lootdust.almostvanilla.util.TagUtils;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Function;

public interface ModDispenseItemBehavior
{
    static void registerDispenseItemBehaviors(HolderGetter<Item> items, Function<ServerLevel, ServerPlayer> fakePlayerProvider) {
        AxeDispenseBehaviourRegistry(items);
        ToolDispenseBehaviourRegistry(items, ItemTags.PICKAXES, BlockTags.MINEABLE_WITH_PICKAXE);
        ToolDispenseBehaviourRegistry(items, ItemTags.SHOVELS, BlockTags.MINEABLE_WITH_SHOVEL);
        HoeDispenseBehaviourRegistry(items);

        MaceDispenseBehaviourRegistry();

        BrushDispenseBehaviourRegistry(fakePlayerProvider);

        SaplingDispenseBehaviourRegistry(items);

        DispenserBlock.registerBehavior(Items.ICE, new IceDispenseItemBehavior(0.75));
        DispenserBlock.registerBehavior(Items.PACKED_ICE, new IceDispenseItemBehavior(0.25));
        DispenserBlock.registerBehavior(Items.BLUE_ICE, new IceDispenseItemBehavior(0.05));
        NameTagDispenseBehaviorRegistry();
    }

    private static void BrushDispenseBehaviourRegistry(Function<ServerLevel, ServerPlayer> fakePlayerProvider) {
        DispenserBlock.registerBehavior(Items.BRUSH, new OptionalDispenseItemBehavior() {
            @Override
            protected @NonNull ItemStack execute(@NonNull BlockSource source, @NonNull ItemStack dispensed) {
                this.setSuccess(false);
                ServerLevel level = source.level();
                BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));

                ServerPlayer fakePlayer = fakePlayerProvider.apply(level);

                Vec3 hitVec = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
                BlockHitResult hitResult = new BlockHitResult(hitVec, source.state().getValue(DispenserBlock.FACING).getOpposite(), targetPos, false);

                resetFakePlayer(fakePlayer, targetPos, source.state().getValue(DispenserBlock.FACING).getOpposite());

                InteractionResult result = fakePlayer.gameMode.useItemOn(
                        fakePlayer,
                        level,
                        ItemStack.EMPTY,
                        InteractionHand.MAIN_HAND,
                        hitResult
                );

                if (result.consumesAction() && dispensed.isDamageableItem()) {
                    this.setSuccess(true);
                }

                resetFakePlayer(fakePlayer, targetPos, source.state().getValue(DispenserBlock.FACING).getOpposite());

                return dispensed;
            }
        });
    }

    private static void resetFakePlayer(ServerPlayer fakePlayer, BlockPos targetPos, Direction facing) {
        fakePlayer.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        fakePlayer.setYRot(facing.toYRot());
        fakePlayer.setXRot(0);

        if (fakePlayer.containerMenu != fakePlayer.inventoryMenu) {
            fakePlayer.closeContainer();
        }

        if (fakePlayer.isUsingItem()) {
            fakePlayer.stopUsingItem();
        }

        fakePlayer.getInventory().clearContent();
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
                                        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5d, pos.getY() + 0.25d, pos.getZ() + 0.5d, remains));
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

    TagKey<Block> COBBLESTONES_TAG = TagUtils.getTagKey(Registries.BLOCK, Identifier.parse("c:cobblestones"));

    private static void MaceDispenseBehaviourRegistry() {
        DispenserBlock.registerBehavior(Items.MACE, new MaceDispenseItemBehavior());
    }

    private static void AxeDispenseBehaviourRegistry(HolderGetter<Item> items) {
        for (Holder<Item> holder : items.getOrThrow(ItemTags.AXES)) {
            DispenserBlock.registerBehavior(holder.value(), new AxeDispenseItemBehavior());
        }
    }

    private static void HoeDispenseBehaviourRegistry(HolderGetter<Item> items) {
        for (Holder<Item> holder : items.getOrThrow(ItemTags.HOES)) {
            DispenserBlock.registerBehavior(holder.value(), new HoeDispenseItemBehavior());
        }
    }

    private static void NameTagDispenseBehaviorRegistry() {
        DispenserBlock.registerBehavior(Items.NAME_TAG, new OptionalDispenseItemBehavior() {
            @Override
            protected @NonNull ItemStack execute(@NonNull BlockSource source, @NonNull ItemStack dispensed) {
                this.setSuccess(false);
                ServerLevel level = source.level();
                BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                Player player = level.getNearestPlayer(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1, false);

                if (player instanceof ServerPlayer) {
                    if (player.isCreative()) {
                        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
                    } else {
                        ((ServerPlayer) player).setGameMode(GameType.CREATIVE);
                    }
                    this.setSuccess(true);
                }

                return dispensed;
            }
        });
    }

    static void insertOrDrop(ServerLevel level, BlockPos pos, DispenserBlockEntity blockEntity, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            ItemStack remains = blockEntity.insertItem(drop);
            if (!remains.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5d, pos.getY() + 0.25d, pos.getZ() + 0.5d, remains));
            }
        }
    }

    static void damageTool(ItemStack dispensed, ServerLevel level) {
        if (!dispensed.getComponents().has(DataComponents.UNBREAKABLE)) {
            dispensed.hurtAndBreak(1, level, null, _ -> { });
        }
    }

    private static void SaplingDispenseBehaviourRegistry(HolderGetter<Item> items) {
        for (Holder<Item> holder : items.getOrThrow(ItemTags.SAPLINGS)) {
            DispenserBlock.registerBehavior(holder.value(), new SaplingDispenseItemBehavior());
        }
    }
}
