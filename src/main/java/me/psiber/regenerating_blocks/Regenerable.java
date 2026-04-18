package me.psiber.regenerating_blocks;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import me.psiber.regenerating_blocks.config.ConfigBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

public class Regenerable {
    public final String namespace;
    public final String blockName;
    public final int regenAfter;
    private final Supplier<Block> sourceBlockSupplier;

    public Regenerable(ConfigBlocks config){
        this.namespace = config.namespace(); // "minecraft"
        this.blockName = config.blockName(); // "gold_ore"
        this.regenAfter = config.regenAfter();

        // We use a supplier because we can't guarantee mod load order.
        // We need to avoid our mod trying to build blocks from a later loaded mod.
        // This ensures we wait until runtime to map.
        this.sourceBlockSupplier = Suppliers.memoize(() -> {
            ResourceLocation originalMaterial = ResourceLocation.fromNamespaceAndPath(namespace, blockName);
            Block found = BuiltInRegistries.BLOCK.get(originalMaterial);

            if (found == Blocks.AIR) {
                // Log a warning if the block is missing when actually accessed
                RegeneratingBlocks.log("Could not find source block " + originalMaterial);
            }
            return found;
        });
    }

    public String GetRegeneratingNameWithNamespace() // "regenerating_blocks:minecraft_gold_ore"
    {
        return RegeneratingBlocks.MOD_ID +":"+ GetRegeneratingBlockName();
    }

    public String GetRegeneratingBlockName() // "minecraft_gold_ore"
    {
        return namespace + "_" + blockName;
    }

    public Block GetSourceBlock() {
        return sourceBlockSupplier.get();
    }
}
