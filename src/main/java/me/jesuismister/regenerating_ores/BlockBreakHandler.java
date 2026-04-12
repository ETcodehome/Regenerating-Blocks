package me.jesuismister.regenerating_ores;

import me.jesuismister.regenerating_ores.blocks.RegeneratingOreBlock;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = "your_mod_id")
public class BlockBreakHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {

        // performance guard - always do nothing if it is not a regenerating block.
        if (!(event.getState().getBlock() instanceof RegeneratingOreBlock)) {
            return;
        }

        RegeneratingOreBlock.log("A block break event is being processed");
        BlockPos pos = event.getPos();

        boolean regenerating = RegenManager.isRegenerating(pos);
        if (regenerating) {

            if (RegenManager.getData(pos).creativeBreak == true) {
                RegeneratingOreBlock.log("Allowed creative break of block");
                return;
            }

            RegeneratingOreBlock.log("Prevented break because block is regenerating.");
            event.setCanceled(true);
            return;
        }


    }
}