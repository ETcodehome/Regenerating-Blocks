package me.jesuismister.regenerating_ores;

import me.jesuismister.regenerating_ores.blocks.RegeneratingOreBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class RegeneratingBlockItem extends BlockItem {
    public RegeneratingBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // This is the "Late Binding" logic.
        // It pulls the pattern from YOUR lang file and the ore name from the SOURCE mod.
        return Component.translatable("text.regenerating_ores.format",
                Component.translatable(((RegeneratingOreBlock)this.getBlock()).block.GetSourceBlock().getDescriptionId()));
    }
}