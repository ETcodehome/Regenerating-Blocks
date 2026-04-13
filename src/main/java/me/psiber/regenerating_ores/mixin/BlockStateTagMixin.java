package me.psiber.regenerating_ores.mixin;

import me.psiber.regenerating_ores.blocks.RegeneratingOreBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Targets the internal base class of all BlockStates
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateTagMixin {

    // Shadows the internal getBlock() method so we can access it within the mixin
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void mirrorSourceTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        Block block = this.getBlock();

        // Check if this block is our custom type
        if (RegeneratingOreBlock.class.isInstance(block)) {
            RegeneratingOreBlock regenBlock = (RegeneratingOreBlock) block;
            // Check if the source block has the tag.
            if (regenBlock.block.GetSourceBlock().builtInRegistryHolder().is(tag)) {
                cir.setReturnValue(true);
            }
        }
    }
}