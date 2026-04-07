package me.jesuismister.regenerating_ores;

import net.neoforged.neoforge.registries.DeferredBlock;

public class Regenerable {
    public String namespace;
    public String blockName;
    public int regenAfter;
    public boolean needStonePick;
    public boolean needIronPick;
    public DeferredBlock deferredBlock;
    public Regenerable(String _namespace, String _blockName, int _regenAfter,boolean _needStonePick, boolean _needIronPick){
        namespace = _namespace; // "minecraft"
        blockName = _blockName; // "gold_ore"
        regenAfter = _regenAfter;
        needStonePick = _needStonePick;
        needIronPick = _needIronPick;
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
}
