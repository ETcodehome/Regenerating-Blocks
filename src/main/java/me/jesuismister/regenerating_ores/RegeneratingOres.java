package me.jesuismister.regenerating_ores;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(RegeneratingOres.MOD_ID)
public class RegeneratingOres {
    public static final String MOD_ID = "regenerating_ores";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RegeneratingOres(IEventBus modEventBus, ModContainer modContainer) {
        // Enregistrer les blocs/items
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        // Enregistrement d'autres fonctionnalités si nécessaire
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(new ItemStack(ModBlocks.REGENERATING_IRON_ORE.get()));
        }
    }
}