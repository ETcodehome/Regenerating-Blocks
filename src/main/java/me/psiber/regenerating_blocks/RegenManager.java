package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import me.psiber.regenerating_blocks.items.ModCreativeModeTabs;
import me.psiber.regenerating_blocks.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegenManager {

    // The "Session Map" - This clears on server reboot.
    // Key: BlockPos (The coordinates)
    // Value: RegenData (Break relevant custom data)
    public static final Map<WorldPos, RegenData> REGENERATING_BLOCKS = new ConcurrentHashMap<>();

    public record WorldPos(ResourceKey<Level> dimension, BlockPos pos) {
        // Records are immutable by default, perfect for Map keys!
    }

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
        Integer regenAfter = ModBlocks.regenTimers.get(id);
        if (regenAfter == null){
            throw new NullPointerException("No regeneration time found for this block!");
        }

        WorldPos key = new WorldPos(level.dimension(), pos.immutable());
        REGENERATING_BLOCKS.put(key, new RegenData(currentTime, regenAfter));
    }

    public static RegenData getData(WorldPos pos) {
        return REGENERATING_BLOCKS.get(pos);
    }

    public static boolean isRegenerating(WorldPos pos) {
        return REGENERATING_BLOCKS.containsKey(pos);
    }

    public static void clearDataAt(WorldPos pos) {
        REGENERATING_BLOCKS.remove(pos);
    }

}