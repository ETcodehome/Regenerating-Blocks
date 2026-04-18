package me.psiber.regenerating_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegenManager {
    public static final Map<WorldPos, RegenData> REGENERATING_BLOCKS = new ConcurrentHashMap<>();
    public record WorldPos(ResourceKey<Level> dimension, BlockPos pos) {}

    public static class RegenData {
        public final long startTime;
        public final long endTime;
        public int lastVisualStage = 10;
        public long lastStageUpdate;
        public boolean creativeBreak;
        private static final long ticksPerSecond = 20L;

        public RegenData(long startTime, int secondsToRegen) {
            this.startTime = startTime;
            this.creativeBreak = false;
            this.endTime = startTime + (secondsToRegen * ticksPerSecond);
            this.lastStageUpdate = startTime;
        }
    }

    public static void cacheBreakData(Level level, BlockPos pos) {
        long currentTime = level.getGameTime();
        BlockState state = level.getBlockState(pos);
        String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        Integer regenAfter = RegeneratingBlocks.regenTimers.get(id);
        if (regenAfter == null){
            throw new NullPointerException("No regeneration time found for this block!");
        }

        WorldPos key = new WorldPos(level.dimension(), pos.immutable());
        RegeneratingBlocks.log("Cached break: " + key);
        REGENERATING_BLOCKS.put(key, new RegenData(currentTime, regenAfter));
    }

    public static boolean isRegenerating(WorldPos pos) {
        return REGENERATING_BLOCKS.containsKey(pos);
    }

}