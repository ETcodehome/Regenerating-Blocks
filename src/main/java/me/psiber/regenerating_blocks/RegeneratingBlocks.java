package me.psiber.regenerating_blocks;

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
    public static HashMap<String, Regenerable> supportedBlocks;
    public static HashMap<String, Integer> regenTimers;
    public static HashSet<Block> supportedOriginalBlocks;

    public RegeneratingBlocks(IEventBus modEventBus, ModContainer modContainer) {

        // load config files
        ConfigManager.load();
        List<Regenerable> blocksFromConfig = ConfigManager.getSupportedBlocks().stream()
                .map(Regenerable::new)
                .toList();

        // Populate a lookup table so we can get all configuration data from blockstates
        supportedBlocks = new HashMap<String, Regenerable>();
        supportedOriginalBlocks = new HashSet<Block>();
        regenTimers = new HashMap<String, Integer>();
        for (Regenerable block : blocksFromConfig )
        {
            supportedBlocks.put(block.GetRegeneratingNameWithNamespace(), block);
            supportedOriginalBlocks.add(block.GetSourceBlock());
            regenTimers.put(block.namespace + ":" + block.blockName, block.regenAfter);
        }

        // append virtual resources
        modEventBus.addListener(PackFinderHandler::register);
    }

    public static void log(String s){

        // Guard against doing the expensive stack walk if logging is disabled
        if (!ConfigManager.getSettings().verboseLogging()){ return; }

        String callerName = StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1) // Skip the current method
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknown"));

        System.out.println("[Regenerating Blocks][" + callerName +"] " + s);
    }
}
