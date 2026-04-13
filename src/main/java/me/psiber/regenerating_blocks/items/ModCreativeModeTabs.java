package me.psiber.regenerating_blocks.items;

import me.psiber.regenerating_blocks.RegeneratingBlocks;
import me.psiber.regenerating_blocks.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RegeneratingBlocks.MOD_ID);

    public static final Supplier<CreativeModeTab> TABS = CREATIVE_MODE_TAB.register("regenerating_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Blocks.DIAMOND_ORE))
                    .title(Component.translatable("creativetab.regenerating_blocks.regenerating_blocks_tab_name"))
                    .displayItems(
                        (itemDisplayParameters, output) -> {
                            ModBlocks.supportedBlocks.values().forEach(block -> output.accept(block.deferredBlock));
                        }
                    ).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}