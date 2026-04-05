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
        namespace = _namespace;
        blockName = _blockName;
        regenAfter = _regenAfter;
        needStonePick = _needStonePick;
        needIronPick = _needIronPick;
    }

    public String GetCleanName(){
        return blockName.replace("_", " ");
    }
}
