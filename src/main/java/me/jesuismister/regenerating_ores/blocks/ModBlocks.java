package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.items.ModItems;
import me.jesuismister.regenerating_ores.RegeneratingOres;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(RegeneratingOres.MOD_ID);

    public static final DeferredBlock<Block> REGENERATING_COAL_ORE = registerBlock("regenerating_coal_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_COPPER_ORE = registerBlock("regenerating_copper_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_DIAMOND_ORE = registerBlock("regenerating_diamond_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_EMERALD_ORE = registerBlock("regenerating_emerald_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_GOLD_ORE = registerBlock("regenerating_gold_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_IRON_ORE = registerBlock("regenerating_iron_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_LAPIS_ORE = registerBlock("regenerating_lapis_ore",
            RegeneratingOreBlock::new);

    public static final DeferredBlock<Block> REGENERATING_REDSTONE_ORE = registerBlock("regenerating_redstone_ore",
            RegeneratingOreBlock::new);

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
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
