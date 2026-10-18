package com.lootdust.almostvanilla.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin extends BaseEntityBlock {
    protected JukeboxBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void almostvanilla$alwaysTick(Level level,
                                                                  BlockState blockState,
                                                                  BlockEntityType<T> type,
                                                                  CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if (type == BlockEntityTypes.JUKEBOX) {
            cir.setReturnValue(createTickerHelper(type, BlockEntityTypes.JUKEBOX, JukeboxBlockEntity::tick));
        }
    }
}
