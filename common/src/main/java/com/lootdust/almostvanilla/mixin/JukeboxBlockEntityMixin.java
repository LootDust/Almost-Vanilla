package com.lootdust.almostvanilla.mixin;

import com.lootdust.almostvanilla.api.JukeboxPlaylistAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalInt;

@SuppressWarnings({"DataFlowIssue", "OptionalUsedAsFieldOrParameterType", "SameParameterValue"})
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements JukeboxPlaylistAccess {
    @Shadow
    private ItemStack item;
    @Shadow @Final
    private JukeboxSongPlayer jukeboxSongPlayer;

    @Unique
    private static final TagKey<Block> jukeboxBlockEntity$JUKEBOX_CONTAINERS = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("jukebox_containers"));
    @Unique
    private OptionalInt jukeboxBlockEntity$playlistPosition = OptionalInt.empty();

    protected JukeboxBlockEntityMixin(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.JUKEBOX, worldPosition, blockState);
    }

    @Shadow
    public abstract ItemStack getTheItem();
    @Shadow
    public abstract void setTheItem(ItemStack itemStack);

    @Inject(method = "onSongChanged", at = @At("HEAD"))
    private void jukeboxBlockEntity$onSongChanged(CallbackInfo ci) {
        if (!this.jukeboxSongPlayer.isPlaying() && jukeboxBlockEntity$shouldLoop())
            jukeboxBlockEntity$tryToAdvancePlaylist(true);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void jukeboxBlockEntity$tick(Level level, BlockPos blockPos, BlockState blockState, JukeboxBlockEntity jukebox, CallbackInfo ci) {
        if (!blockState.getValue(JukeboxBlock.HAS_RECORD)) {
            if (!level.isClientSide() && level.getBlockState(blockPos.above()).is(jukeboxBlockEntity$JUKEBOX_CONTAINERS)) {
                ((JukeboxPlaylistAccess)jukebox).jukeboxBlockEntity$tryToAdvancePlaylist(false);
            }
            ci.cancel();
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    protected void jukeboxBlockEntity$loadAdditional(ValueInput input, CallbackInfo ci) {
        Optional<Integer> newPlaylistPosition = input.getInt("playlist_position");
        this.jukeboxBlockEntity$playlistPosition = newPlaylistPosition.map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    protected void jukeboxBlockEntity$saveAdditional(ValueOutput output, CallbackInfo ci) {
        if (this.jukeboxBlockEntity$playlistPosition.isPresent())
            output.putInt("playlist_position", this.jukeboxBlockEntity$playlistPosition.getAsInt());
    }

    @Unique
    private boolean jukeboxBlockEntity$shouldLoop() {
        return (jukeboxBlockEntity$isMusicDisc(this.item) && jukeboxBlockEntity$isInPlaylistMode(this.level, this.worldPosition));
    }

    @Unique
    private static boolean jukeboxBlockEntity$isInPlaylistMode(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState blockAbove = level.getBlockState(above);
        BlockEntity entityAbove = level.getBlockEntity(above);
        return (blockAbove.is(jukeboxBlockEntity$JUKEBOX_CONTAINERS) && entityAbove instanceof Container && (
                !(blockAbove.getBlock() instanceof ChestBlock) ||
                ChestBlock.getBlockType(blockAbove) == DoubleBlockCombiner.BlockType.SINGLE));
    }

    @Unique
    private static boolean jukeboxBlockEntity$isMusicDisc(ItemStack item) {
        return item.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    @Unique
    private static OptionalInt jukeboxBlockEntity$findNextPlayListPosition(Container container, int startPosition) {
        for (int offset = 1; offset <= container.getContainerSize(); offset++) {
            int slot = (startPosition + offset) % container.getContainerSize();
            if (jukeboxBlockEntity$isMusicDisc(container.getItem(slot)))
                return OptionalInt.of(slot);
        }
        return OptionalInt.empty();
    }

    @Unique
    private void jukeboxBlockEntity$replayCurrentSong() {
        setTheItem(this.item);
    }

    @Unique
    private void jukeboxBlockEntity$advancePlaylist(Container container, int newPosition) {
        ItemStack recordItem = container.getItem(newPosition);
        container.setItem(newPosition, getTheItem());
        setTheItem(recordItem);
    }

    @Unique
    @Override
    public void jukeboxBlockEntity$tryToAdvancePlaylist(boolean replayCurrentSongOtherwise) {
        BlockEntity blockEntity = this.level.getBlockEntity(this.worldPosition.above());
        if (blockEntity instanceof Container container) {
            this.jukeboxBlockEntity$playlistPosition = jukeboxBlockEntity$findNextPlayListPosition(container, this.jukeboxBlockEntity$playlistPosition.orElse(-1));
            this.jukeboxBlockEntity$playlistPosition.ifPresentOrElse(newPosition -> jukeboxBlockEntity$advancePlaylist(container, newPosition),
                    replayCurrentSongOtherwise ? this::jukeboxBlockEntity$replayCurrentSong : (() -> {}));
        }
    }
}
