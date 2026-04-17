package me.psiber.regenerating_blocks.blocks;

import me.psiber.regenerating_blocks.RegeneratingBlocks;
import me.psiber.regenerating_blocks.Regenerable;
import me.psiber.regenerating_blocks.RegeneratingBlockItem;
import me.psiber.regenerating_blocks.items.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;

public class ModBlocks {

    public static HashMap<String, Regenerable> supportedBlocks;
    public static HashMap<String, Integer> regenTimers;
    public static HashSet<Block> supportedOriginalBlocks;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(RegeneratingBlocks.MOD_ID);

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = (DeferredBlock<T>) BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new RegeneratingBlockItem(block.get(),
                new Item.Properties().rarity(Rarity.EPIC)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
