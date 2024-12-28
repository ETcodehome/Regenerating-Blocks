package me.jesuismister.regenerating_ores;

import me.jesuismister.regenerating_ores.blocks.ModBlocks;
import me.jesuismister.regenerating_ores.items.ModCreativeModeTabs;
import me.jesuismister.regenerating_ores.items.ModItems;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RegeneratingOres.MOD_ID)
public class RegeneratingOres {
    public static final String MOD_ID = "regenerating_ores";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RegeneratingOres(IEventBus modEventBus, ModContainer modContainer) {
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }
}