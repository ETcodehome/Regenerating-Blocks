package me.psiber.regenerating_ores.mixin;

import me.psiber.regenerating_ores.blocks.RegeneratingOreBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin can be validated as working or not by running similar command on a regenerating ore block at runtime
// /execute if block x y z #minecraft:mineable/pickaxe run say Tag Mirroring Active

@Mixin(net.minecraft.core.Holder.Reference.class)
public abstract class HolderTagMixin<T> {
    @Shadow public abstract T value();

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void mirrorTags(TagKey<T> tag, CallbackInfoReturnable<Boolean> cir) {
        if (this.value() instanceof RegeneratingOreBlock regenBlock) {
            if (regenBlock.block.GetSourceBlock().builtInRegistryHolder().is((TagKey<Block>) tag)) {
                cir.setReturnValue(true);
            }
        }
    }
}
