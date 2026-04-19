package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BlockHighlightHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        // Disable particles if user has opted out via config setting
        if (!ConfigManager.getSettings().showParticles()) { return; }

        // Guard against rendering at the wrong time during the render pipeline
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        // Guard against rendering when there is no valid hit result
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (!(hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)) {
            return;
        }

        // Guard against rendering anything unless the targeted block is regenerating
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHitResult.getBlockPos();
        RegenManager.WorldPos key = new RegenManager.WorldPos(event.getCamera().getEntity().level().dimension(), targetPos);
        if (!RegenManager.isRegenerating(key)) {
            return;
        }

        spawnEnchantThroughCenter(event.getCamera().getEntity().level(), key.pos());

        // Handy block highlighting logic
        /*

        // Translate to the block's position relative to the camera
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(targetPos.getX() - camera.x,
                targetPos.getY() - camera.y,
                targetPos.getZ() - camera.z);

        // 1. Get the current time (use partialTick for smoother transitions)
        long time = Minecraft.getInstance().level.getGameTime();
        DeltaTracker partialTick = event.getPartialTick(); // From your render event
        float hue = ((time + partialTick.getGameTimeDeltaTicks()) % 200) / 200.0f; // Cycles every 10 seconds

        // 2. Convert HSV to RGB
        // Saturation = 1.0 (vibrant), Value = 1.0 (bright)
        int color = Mth.hsvToRgb(hue, 1.0f, 1.0f);

        // 3. Extract RGBA components
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.5f; // Full opacity

        // Render a glowing box (simplified example)
        LevelRenderer.renderLineBox(poseStack,
                Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()),
                0, 0, 0, 1, 1, 1, // Dimensions
                r, g, b,a); // RGBA (Yellow)

        poseStack.popPose();

        */

    }

    public static void spawnEnchantThroughCenter(Level level, BlockPos pos) {

        // Disable particles if user has opted out via config setting
        if (!ConfigManager.getSettings().showParticles()) { return; }

        RandomSource random = level.getRandom();

        // Define the true center of the block
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // Determine a random starting distance (e.g., 1.5 to 2.0 blocks away)
        double distance = 1.5 + (random.nextDouble() * 0.5);

        // Generate a random spherical direction for the start point
        double theta = random.nextDouble() * Math.PI * 2; // Azimuth
        double phi = Math.acos(2.0 * random.nextDouble() - 1.0); // Zenith

        // Calculate where the particle starts
        double offsetX = distance * Math.sin(phi) * Math.cos(theta);
        double offsetY = distance * Math.sin(phi) * Math.sin(theta);
        double offsetZ = distance * Math.cos(phi);

        // The particle will fly from (center + offset) toward (center)
        level.addParticle(
                ParticleTypes.ENCHANT,
                centerX, centerY, centerZ,
                offsetX, offsetY, offsetZ
        );
    }

}
