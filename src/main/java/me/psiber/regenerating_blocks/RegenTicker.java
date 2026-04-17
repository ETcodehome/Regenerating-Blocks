package me.psiber.regenerating_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID)
public class RegenTicker {

    public static final int salt = 5037;
    private static final int maxTicksBetweenUpdates = 380;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Guard against updates more than 4 times per second (20 tps / 5 = 4)
        // 250ms maximum frequency
        final long currentTime = level.getGameTime();
        if (currentTime % 5 != 0) return;

        // Use an iterator or removeIf for thread-safe modification
        RegenManager.REGENERATING_BLOCKS.entrySet().removeIf(entry -> {
            RegenManager.WorldPos pos = entry.getKey();
            RegenManager.RegenData data = entry.getValue();

            // Check if we are finished
            if (currentTime >= data.endTime) {
                level.destroyBlockProgress(pos.hashCode() + salt, pos.pos(), -1);
                spawnSubtleEffect(level, pos.pos());
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
                level.levelEvent(LevelEvent.LAVA_FIZZ, pos.pos(), Block.getId(event.getLevel().getBlockState(pos.pos())));
            }

            // Only send an update if the stage has changed (optimization)
            boolean needsCrackRefresh = Math.abs(currentTime - data.lastStageUpdate) >= maxTicksBetweenUpdates;
            if (needsCrackRefresh || needsStageUpdate) {
                level.destroyBlockProgress(pos.hashCode() + salt, pos.pos(), currentStage);
                data.lastStageUpdate = level.getGameTime();
            }

            return false;
        });
    }

    public static void handleBreak() {

    }

    private static void sendRandomParticles(ServerLevel level, BlockPos pos){
        var random = level.getRandom();

        // Base Values
        float baseRed = 0.85f;
        float baseGreen = 0.85f;
        float baseBlue = 0.85f;
        float baseScale = 0.5f;

        // We'll spawn individual particles to ensure each one has unique randomization
        for (int i = 0; i < 8; i++) {
            // Calculate +/- 10% variance (random.nextFloat() is 0.0 to 1.0)
            float vFactor = 0.5f + (random.nextFloat() * 0.5f); // Results in 0.5 to 1.0

            // Apply variance to color and scale
            float r = Math.max(0, Math.min(1, baseRed * vFactor));
            float g = Math.max(0, Math.min(1, baseGreen * vFactor));
            float b = Math.max(0, Math.min(1, baseBlue * vFactor));
            float scale = baseScale * vFactor;

            DustParticleOptions randomizedGlow = new DustParticleOptions(new Vector3f(r, g, b), scale);

            // Send individual particle with randomized spread
            level.sendParticles(randomizedGlow,
                    pos.getX() + 0.5 + (random.nextGaussian() * 0.2),
                    pos.getY() + 0.5 + (random.nextGaussian() * 0.2),
                    pos.getZ() + 0.5 + (random.nextGaussian() * 0.2),
                    1, 0.3, 0.3, 0.3, 0.005);
        }
    }

    private static void spawnSubtleEffect(ServerLevel level, BlockPos pos) {

        // Disable particles if user has opted out via config setting
        if (!ConfigManager.getSettings().showParticles()) { return; }

        double offset = 0.25;
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() -0.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX()  +1.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() +0.5, pos.getY() + 0.5, pos.getZ() -0.2,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() +1.2,
                5, offset, offset, offset, 0.1);
    }
}
