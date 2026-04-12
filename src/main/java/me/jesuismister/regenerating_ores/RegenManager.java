package me.jesuismister.regenerating_ores;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegenManager {

    // The "Session Map" - This clears on server reboot.
    // Key: BlockPos (The coordinates)
    // Value: RegenData (Break relevant custom data)
    private static final Map<BlockPos, RegenData> REGENERATING_BLOCKS = new ConcurrentHashMap<>();

    public static class RegenData {
        public final long startTime;
        public int lastVisualStage = 9;
        public boolean creativeBreak;
        public boolean playerBreak;

        public RegenData(long startTime, boolean playerBreak) {
            this.startTime = startTime;
            this.creativeBreak = false;
            this.playerBreak = playerBreak;
        }
    }

    public static void cacheBreakData(Level level, BlockPos pos, boolean playerBreak) {
        long currentTime = level.getGameTime();
        REGENERATING_BLOCKS.put(pos.immutable(), new RegenData(currentTime, playerBreak));
    }

    public static RegenData getData(BlockPos pos) {
        return REGENERATING_BLOCKS.get(pos);
    }

    public static boolean isRegenerating(BlockPos pos) {
        return REGENERATING_BLOCKS.containsKey(pos);
    }

    public static void clearDataAt(BlockPos pos) {
        REGENERATING_BLOCKS.remove(pos);
    }

}