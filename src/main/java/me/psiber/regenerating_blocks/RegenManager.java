package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import me.psiber.regenerating_blocks.items.ModCreativeModeTabs;
import me.psiber.regenerating_blocks.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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