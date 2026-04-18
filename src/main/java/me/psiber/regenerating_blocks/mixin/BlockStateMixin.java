package me.psiber.regenerating_blocks.mixin;

import net.minecraft.core.BlockPos;
import me.psiber.regenerating_blocks.RegenManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void onGetDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        // 'this' refers to the BlockState being checked
        BlockState state = (BlockState) (Object) this;

        if (level instanceof Level world) {
            RegenManager.WorldPos key = new RegenManager.WorldPos(world.dimension(), pos.immutable());
            if (RegenManager.isRegenerating(key)) {
                // Returning -1.0F makes the block technically 'bedrock-level' unbreakable
                // Create drills will skip blocks with -1.0 hardness.
                cir.setReturnValue(-1.0F);
            }
        }
    }
}