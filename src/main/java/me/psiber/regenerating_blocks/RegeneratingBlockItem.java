package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.RegeneratingBlock;
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
        return Component.translatable("text.regenerating_blocks.format",
                Component.translatable(((RegeneratingBlock)this.getBlock()).block.GetSourceBlock().getDescriptionId()));
    }
}