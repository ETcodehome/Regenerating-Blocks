package me.psiber.regenerating_blocks.mixin;

import me.psiber.regenerating_blocks.BlockBreakHandler;
import me.psiber.regenerating_blocks.RegenManager;
import me.psiber.regenerating_blocks.RegenTicker;
import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class SetBlockMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void onSetBlock(BlockPos pos, BlockState newState, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {

        // only do any processing if we're the server
        Level level = (Level) (Object) this;
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        // exit quickly if block is not a regenerating block, do standard behavior
        BlockState oldState = level.getBlockState(pos);
        if (!ModBlocks.supportedOriginalBlocks.contains(oldState.getBlock())) {
            return;
        }

        RegeneratingBlock.log("A block was set to something");
        BlockBreakHandler.handleBlockBreakEvent(serverLevel, pos, oldState, false);
    }
}