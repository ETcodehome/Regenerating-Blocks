package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID)
public class RegenTicker {

    public static final int salt = 5037;
    private static final int maxTicksBetweenUpdates = 380;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {

        if (!(event.getLevel() instanceof ServerLevel level)){
            return;
        }

        // Guard against updates more than 4 times per second (20 tps / 5 = 4)
        // 250ms maximum frequency
        final long currentTime = level.getGameTime();
        if (currentTime % 5 != 0) return;

        // Use an iterator or removeIf for thread-safe modification
        RegenManager.REGENERATING_BLOCKS.entrySet().removeIf(entry -> {
            RegenManager.WorldPos key = entry.getKey();
            RegenManager.RegenData data = entry.getValue();
            ServerLevel keyLevel = level.getServer().getLevel(key.dimension());

            // Check if we are finished
            if (currentTime >= data.endTime) {
                keyLevel.destroyBlockProgress(key.hashCode() + salt, key.pos(), -1);
                spawnFinishEffect(keyLevel, key.pos());
                return true; // removes from the block set
            }

            // Calculate the current visual stage (0 to 9)
            long totalDuration = data.endTime - data.startTime;
            long elapsed = currentTime - data.startTime;

            // Percentage of progress (0.0 to 1.0)
            float progress = (float) elapsed / totalDuration;

            // Map 0.0-1.0 to 9-0 (so cracks vanish over time)
            final int currentStage = 9 - (int)(progress * 10);

            boolean needsStageUpdate = currentStage != data.lastVisualStage;
            if (needsStageUpdate){
                data.lastVisualStage = currentStage;
            }

            // Only send an update if the stage has changed (optimization)
            boolean needsCrackRefresh = Math.abs(currentTime - data.lastStageUpdate) >= maxTicksBetweenUpdates;
            if (needsCrackRefresh || needsStageUpdate) {
                keyLevel.destroyBlockProgress(key.hashCode() + salt, key.pos(), currentStage);
                data.lastStageUpdate = keyLevel.getGameTime();
            }

            return false;
        });
    }

    private static void spawnFinishEffect(ServerLevel level, BlockPos pos) {

        // Disable particles if user has opted out via config setting
        if (!ConfigManager.getSettings().showParticles()) { return; }

        final SimpleParticleType particle = ParticleTypes.SCRAPE;
        final double offset = 0.25;

        level.sendParticles(particle,
                pos.getX() -0.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(particle,
                pos.getX()  +1.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(particle,
                pos.getX() +0.5, pos.getY() + 0.5, pos.getZ() -0.2,
                5, offset, offset, offset, 0.1);
        level.sendParticles(particle,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() +1.2,
                5, offset, offset, offset, 0.1);
    }
}
