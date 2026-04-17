package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import static me.psiber.regenerating_blocks.ChunkMirrorHandler.getMirrorKey;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID)
public class BlockBreakHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        // Guard as cheap as possible to ignore "non regenerating blocks"
        Block block = event.getState().getBlock();
        if (!ModBlocks.supportedOriginalBlocks.contains(block)){
            RegeneratingBlock.log("ignoring, not a regenerating block.");
            return;
        }

        // on block break is normally only ever called by players. Machines don't call this.
        boolean shouldCancel = handleBlockBreakEvent((ServerLevel) event.getLevel(), event.getPos(), event.getState(), event.getPlayer().isCreative());
        if (shouldCancel){
            event.setCanceled(true);
        }
    }

    public static boolean handleBlockBreakEvent(ServerLevel level, BlockPos pos, BlockState oldState, Boolean creativeBreak){

        // Guard against no mirror dimension being mapped for this dimension
        ResourceKey<Level> mirrorKey = getMirrorKey(level.dimension());
        if (mirrorKey == null){
            RegeneratingBlock.log("Ignoring, no mirror key exists for this dimension");
            return false;
        }

        // Guard against the actual world mapped not being resolved
        MinecraftServer server = level.getServer();
        ServerLevel mirrorLevel = server.getLevel(mirrorKey);
        if (mirrorLevel == null){
            RegeneratingBlock.log("Ignoring, mapped mirror dimension was unresolvable");
            return false;
        }

        // Guard against player placed blocks regenerating
        BlockState mirrorBlock = mirrorLevel.getBlockState(pos);
        Boolean naturallySpawned = mirrorBlock.is(oldState.getBlock());
        if (!naturallySpawned) {
            RegeneratingBlock.log("Ignoring, block was player placed (never respawns, doesn't need special handling)");
            return false;
        }

        // Guard against unbreakable blocks. Allow creative mode players to change the state of a regenerating ore.
        RegenManager.WorldPos key = new RegenManager.WorldPos(level.dimension(), pos.immutable());
        boolean regenerating = RegenManager.isRegenerating(key);
        if (regenerating && creativeBreak) {
            RegeneratingBlock.log("Allowed, break source was a creative mode player");
            return false;
        }

        // Guard against breaking a regenerating block
        if (regenerating) {
            RegeneratingBlock.log("Cancelled, block was currently in regenerating state.");
            RegeneratingBlock.log("startTime:" + String.valueOf(RegenManager.getData(key).startTime));
            RegeneratingBlock.log("endTime:" + String.valueOf(RegenManager.getData(key).endTime));
            RegeneratingBlock.log("currentTime:" + String.valueOf(level.getGameTime()));
            return true; // do cancel if possible
        }

        // Genuine break event. Add to regeneration table.
        RegeneratingBlock.log("Genuine break, added block to regeneration tracker.");
        RegenManager.cacheBreakData(level, pos);
        level.levelEvent(2001, pos, Block.getId(oldState));

        // do a callback to reset the block back to its current state.
        // This lets the "break" complete (doing drops) but then resets the broken block.
        level.getServer().execute(() -> {
            RegeneratingBlock.log("Restoring broken block to previous block state.");
            level.setBlock(pos, oldState, 3);
        });
        return false;
    }
}