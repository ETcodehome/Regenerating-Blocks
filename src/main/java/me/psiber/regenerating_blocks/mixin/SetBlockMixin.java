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
            RegeneratingBlock.log("Ignoring, oldstate " + oldState + " is not a regenerating flagged block.");
            return;
        }

        // Only care if we are moving FROM a supported block to not the regenerating state
        boolean stateMutating = newState.getBlock() != oldState.getBlock();
        if (!stateMutating) return;
        if (stateMutating) {
            RegeneratingBlock.log("Block changing into " + newState);
            //BlockBreakHandler.handleBlockBreakEvent(serverLevel, pos, oldState, newState, false);
            level.setBlock(pos, oldState, 19, 0); // set it back to the regenerating block it should be
            return;
        }

        // Guard against triggering behavior if we're already regenerating
        RegenManager.WorldPos key = new RegenManager.WorldPos(level.dimension(), pos.immutable());
        if (RegenManager.isRegenerating(key)) {
            level.setBlock(pos, oldState, 19, 0);
            RegeneratingBlock.log("Regenerating, making the new state the old state");
            return;
        }



        // do nothing I guess?
    }
}