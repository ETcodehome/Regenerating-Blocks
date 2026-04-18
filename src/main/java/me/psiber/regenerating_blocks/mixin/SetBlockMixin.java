package me.psiber.regenerating_blocks.mixin;

import me.psiber.regenerating_blocks.ConfigManager;
import me.psiber.regenerating_blocks.RegenManager;
import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.psiber.regenerating_blocks.ChunkMirrorHandler.getMirrorKey;

@Mixin(Level.class)
public abstract class SetBlockMixin {

    // "Shadow" the field from the Level class so we can reference it
    @Shadow
    public boolean isClientSide;

    @Inject(
            method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPreDestroyBlock(BlockPos pos, boolean dropBlock, Entity breaker, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {

        RegeneratingBlock.log("ON PRE DESTROY");

        ServerLevel level = (ServerLevel) (Object) this;

        // 1. Resolve State - Get the block currently at the position
        BlockState oldState = level.getBlockState(pos);

        // Performance Guard: Exit if not a block we care about
        if (!ModBlocks.supportedOriginalBlocks.contains(oldState.getBlock())) {
            return;
        }

        // 2. Logic Guard: If currently regenerating, never allow break (and thus no drops)
        RegenManager.WorldPos key = new RegenManager.WorldPos(level.dimension(), pos.immutable());
        if (RegenManager.isRegenerating(key)) {
            RegeneratingBlock.log("Intercepted break: Block is actively regenerating. Aborting destruction.");
            cir.setReturnValue(false);
            return;
        }

    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUniversalSetBlock(BlockPos pos, BlockState newState, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        BlockState oldState = level.getBlockState(pos);
        if (!ModBlocks.supportedOriginalBlocks.contains(oldState.getBlock())){
            RegeneratingBlock.log("Block isn't supported, doing nothing");
            return;
        }

        //////////////////////////// CLIENT /////////////////////////////////////////

        // Prevents the "Flicker" from drills/players by telling the client the block can't be broken.
        if (level.isClientSide()) {
            RegenManager.WorldPos key = new RegenManager.WorldPos(((Level)(Object)this).dimension(), pos.immutable());
            if (RegenManager.isRegenerating(key)) {
                RegeneratingBlock.log("Client realised block is regenerating and ignored the break.");
                cir.setReturnValue(false);
                return;
            }
            // Note: We don't check the Mirror here because it's Server-only.
            RegeneratingBlock.log("Client saw a non regenerating break event and took no further action.");
            return;
        }

        //////////////////////////// SERVER /////////////////////////////////////////

        if (level instanceof ServerLevel serverLevel) {

            RegenManager.WorldPos key = new RegenManager.WorldPos(level.dimension(), pos.immutable());

            // Guard - If actively regenerating, abort immediately
            if (RegenManager.isRegenerating(key)) {
                RegeneratingBlock.log("Server realised block is regenerating and actively prevented the break.");
                cir.setReturnValue(false);

                return;
            }

            // Guard - Resolve Mirror
            ResourceKey<Level> mirrorKey = getMirrorKey(level.dimension());
            ServerLevel mirrorLevel = (mirrorKey != null) ? level.getServer().getLevel(mirrorKey) : null;

            if (mirrorLevel == null){
                RegeneratingBlock.log("Server couldn't resolve a mirror level.");
                return;
            }

            // Guard - Naturally Spawned Check
            if (!mirrorLevel.getBlockState(pos).is(oldState.getBlock())) {
                RegeneratingBlock.log("Server did nothing. Mirror world block didn't match block being broken.");
                return;
            }

            // expected standard break pathway
            if (newState.isAir()){
                RegenManager.cacheBreakData(level, pos);
                level.levelEvent(2001, pos, Block.getId(oldState));
                cir.setReturnValue(false);
                RegeneratingBlock.log("Allowed break, but prevented block destruction");
                return;
            }

            // block is transitioning to a different block
            boolean disableTransitions = ConfigManager.getSettings().disableTransitions();
            if (disableTransitions){
                cir.setReturnValue(false);
                RegeneratingBlock.log("Obeyed config and prevented transition to " + newState);
                return;
            }
        }
    }

}