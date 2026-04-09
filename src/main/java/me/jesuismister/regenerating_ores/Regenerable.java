package me.jesuismister.regenerating_ores;

import com.google.common.cache.Cache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.core.LayeredRegistryAccess;

public class Regenerable {
    public String namespace;
    public String blockName;
    public int regenAfter;
    public boolean needStonePick;
    public boolean needIronPick;
    public DeferredBlock deferredBlock;
    public Block sourceBlock;
    public Regenerable(String _namespace, String _blockName, int _regenAfter){
        namespace = _namespace; // "minecraft"
        blockName = _blockName; // "gold_ore"
        regenAfter = _regenAfter;

        ResourceLocation originalMaterial = ResourceLocation.parse(GetOriginalNameWithNamespace());
        sourceBlock = BuiltInRegistries.BLOCK.get(originalMaterial);
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

    public Block GetSourceBlock()
    {
        return sourceBlock;
    }


}
