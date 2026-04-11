package me.jesuismister.regenerating_ores;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

public class Regenerable {
    public final String namespace;
    public final String blockName;
    public final int regenAfter;
    public DeferredBlock<?> deferredBlock;
    // Use a Memoized Supplier: it runs once and caches the result
    private final Supplier<Block> sourceBlockSupplier;

    public Regenerable(RegenerableConfig config){
        this.namespace = config.namespace(); // "minecraft"
        this.blockName = config.blockName(); // "gold_ore"
        this.regenAfter = config.regenAfter();

        // We use a supplier because we can't guarantee mod load order.
        // We need to avoid our mod trying to build ores from a later loaded mod.
        // This ensures we wait until runtime to map.
        this.sourceBlockSupplier = Suppliers.memoize(() -> {
            ResourceLocation originalMaterial = ResourceLocation.fromNamespaceAndPath(namespace, blockName);
            Block found = BuiltInRegistries.BLOCK.get(originalMaterial);

            if (found == Blocks.AIR) {
                // Log a warning if the block is missing when actually accessed
                System.err.println("RegenOres: Could not find source block " + originalMaterial);
            }
            return found;
        });
    }

    public String GetPresentationName() // "Regenerating gold ore"
    {
        return "Regenerating " + blockName.replace("_", " ");
    }

    public String GetOriginalNameWithNamespace() // "minecraft:gold_ore"
    {
        return namespace +":"+ blockName;
    }

    public String GetRegeneratingNameWithNamespace() // "regenerating_ores:minecraft_gold_ore"
    {
        return RegeneratingOres.MOD_ID +":"+ GetRegeneratingBlockName();
    }

    public String GetRegeneratingBlockName() // "minecraft_gold_ore"
    {
        return namespace + "_" + blockName;
    }

    public Block GetSourceBlock() {
        return sourceBlockSupplier.get();
    }
}
