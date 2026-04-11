package me.jesuismister.regenerating_ores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RegenerableConfig(String namespace, String blockName, int regenAfter) {
    public static final Codec<RegenerableConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("namespace").forGetter(RegenerableConfig::namespace),
            Codec.STRING.fieldOf("block_name").forGetter(RegenerableConfig::blockName),
            Codec.INT.fieldOf("regen_after").forGetter(RegenerableConfig::regenAfter)
    ).apply(inst, RegenerableConfig::new));
}