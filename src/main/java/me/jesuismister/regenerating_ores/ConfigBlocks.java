package me.jesuismister.regenerating_ores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ConfigBlocks(String namespace, String blockName, int regenAfter) {
    public static final Codec<ConfigBlocks> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("namespace").forGetter(ConfigBlocks::namespace),
            Codec.STRING.fieldOf("block_name").forGetter(ConfigBlocks::blockName),
            Codec.INT.fieldOf("regen_after").forGetter(ConfigBlocks::regenAfter)
    ).apply(inst, ConfigBlocks::new));

    public static final List<ConfigBlocks> DEFAULT = List.of(
            new ConfigBlocks("minecraft", "copper_ore", 20),
            new ConfigBlocks("minecraft", "diamond_ore", 300),
            new ConfigBlocks("minecraft", "emerald_ore", 60),
            new ConfigBlocks("minecraft", "gold_ore", 30),
            new ConfigBlocks("minecraft", "iron_ore", 20),
            new ConfigBlocks("minecraft", "lapis_ore", 30),
            new ConfigBlocks("minecraft", "redstone_ore", 30),
            new ConfigBlocks("minecraft", "stone", 5),
            new ConfigBlocks("minecraft", "obsidian", 5)
    );
}