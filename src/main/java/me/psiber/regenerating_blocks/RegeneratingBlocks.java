package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
import me.psiber.regenerating_blocks.items.ModCreativeModeTabs;
import me.psiber.regenerating_blocks.items.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;

@Mod(RegeneratingBlocks.MOD_ID)
public class RegeneratingBlocks {
    public static final String MOD_ID = "regenerating_blocks";

    public RegeneratingBlocks(IEventBus modEventBus, ModContainer modContainer) {

        // do registration
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);

        // load config files
        ConfigManager.load();
        List<Regenerable> blocksFromConfig = ConfigManager.getSupportedBlocks().stream()
                .map(Regenerable::new)
                .toList();

        // Populate a lookup table so we can get all configuration data from blockstates
        ModBlocks.supportedBlocks = new HashMap<String, Regenerable>();
        for (Regenerable block : blocksFromConfig )
        {
            ModBlocks.supportedBlocks.put(block.GetRegeneratingNameWithNamespace(), block);
        }

        // append virtual resources
        modEventBus.addListener(PackFinderHandler::register);

        // ready the deferred blocks
        for (Regenerable block : ModBlocks.supportedBlocks.values()) {
            block.deferredBlock = ModBlocks.registerBlock(block.GetRegeneratingBlockName(), () -> new RegeneratingBlock(block, block.regenAfter));
        }
        NeoForge.EVENT_BUS.register(new BlockBreakHandler());

    }

}
