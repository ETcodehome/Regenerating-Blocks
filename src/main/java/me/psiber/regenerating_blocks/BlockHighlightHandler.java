package me.psiber.regenerating_blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BlockHighlightHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Only render during the 'AFTER_PARTICLES' or 'AFTER_TRIPWIRE' stage
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        PoseStack poseStack = event.getPoseStack();
        HitResult hitResult = Minecraft.getInstance().hitResult;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos targetPos = blockHit.getBlockPos();

            // Don't render anything unless the block is regenerating
            RegenManager.WorldPos key = new RegenManager.WorldPos(event.getCamera().getEntity().level().dimension(),targetPos);
            if (!RegenManager.isRegenerating(key)) {
                return;
            }

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
        }
    }

}
