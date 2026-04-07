package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.Regenerable;
import me.jesuismister.regenerating_ores.items.ModItems;
import me.jesuismister.regenerating_ores.RegeneratingOres;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class ModBlocks {

    public static ArrayList<Regenerable> supportedBlocks;
    public static HashMap<String, String> blockMap;
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(RegeneratingOres.MOD_ID);

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = (DeferredBlock<T>) BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(Rarity.EPIC)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
