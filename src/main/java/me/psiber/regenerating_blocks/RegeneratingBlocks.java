package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.config.ConfigManager;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Mod(RegeneratingBlocks.MOD_ID)
public class RegeneratingBlocks {
    public static final String MOD_ID = "regenerating_blocks";
    public static HashMap<String, Integer> regenTimers;
    public static HashSet<String> supportedOriginalBlocks;
    private static boolean loggingActive;

    public RegeneratingBlocks(IEventBus modEventBus, ModContainer modContainer) {

        // load config files
        ConfigManager.load();
        List<Regenerable> blocksFromConfig = ConfigManager.getSupportedBlocks().stream()
                .map(Regenerable::new)
                .toList();
        loggingActive = ConfigManager.getSettings().verboseLogging();

        // Populate a lookup table so we can get all configuration data from blockstates
        supportedOriginalBlocks = new HashSet<String>();
        regenTimers = new HashMap<String, Integer>();
        for (Regenerable block : blocksFromConfig )
        {
            String id = block.namespace + ":" + block.blockName;
            supportedOriginalBlocks.add(id);
            regenTimers.put(id, block.regenAfter);
        }

        // append virtual resources
        modEventBus.addListener(PackFinderHandler::register);
    }

    public static void log(String s){

        // Guard against doing the expensive stack walk if logging is disabled
        if (!loggingActive){return;}

        String callerName = StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1) // Skip the current method
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknown"));

        System.out.println("[Regenerating Blocks][" + callerName +"] " + s);
    }
}
