package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import me.psiber.regenerating_blocks.items.ModCreativeModeTabs;
import me.psiber.regenerating_blocks.items.ModItems;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Mod(RegeneratingBlocks.MOD_ID)
public class RegeneratingBlocks {
    public static final String MOD_ID = "regenerating_blocks";

    public RegeneratingBlocks(IEventBus modEventBus, ModContainer modContainer) {

        // load config files
        ConfigManager.load();
        List<Regenerable> blocksFromConfig = ConfigManager.getSupportedBlocks().stream()
                .map(Regenerable::new)
                .toList();

        // Populate a lookup table so we can get all configuration data from blockstates
        ModBlocks.supportedBlocks = new HashMap<String, Regenerable>();
        ModBlocks.supportedOriginalBlocks = new HashSet<Block>();
        ModBlocks.regenTimers = new HashMap<String, Integer>();
        for (Regenerable block : blocksFromConfig )
        {
            ModBlocks.supportedBlocks.put(block.GetRegeneratingNameWithNamespace(), block);
            ModBlocks.supportedOriginalBlocks.add(block.GetSourceBlock());
            ModBlocks.regenTimers.put(block.namespace + ":" + block.blockName, block.regenAfter);
        }

        // ready the deferred blocks
        for (Regenerable block : ModBlocks.supportedBlocks.values()) {
            block.deferredBlock = ModBlocks.registerBlock(block.GetRegeneratingBlockName(), () -> new RegeneratingBlock(block, block.regenAfter));
        }

        // do registration
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);

        // append virtual resources
        modEventBus.addListener(PackFinderHandler::register);
        //NeoForge.EVENT_BUS.register(BlockBreakHandler.class);

    }

}
