package me.psiber.regenerating_blocks;

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
            // Common Stones (Low Hardness, High Abundance)
            new ConfigBlocks("minecraft", "stone", 3),
            new ConfigBlocks("minecraft", "deepslate", 5),
            new ConfigBlocks("minecraft", "granite", 4),
            new ConfigBlocks("minecraft", "diorite", 4),
            new ConfigBlocks("minecraft", "andesite", 4),
            new ConfigBlocks("minecraft", "tuff", 4),

            // Common Ores (Low-Mid Tier)
            new ConfigBlocks("minecraft", "coal_ore", 15),
            new ConfigBlocks("minecraft", "deepslate_coal_ore", 18),
            new ConfigBlocks("minecraft", "copper_ore", 20),
            new ConfigBlocks("minecraft", "deepslate_copper_ore", 25),
            new ConfigBlocks("minecraft", "iron_ore", 45),
            new ConfigBlocks("minecraft", "deepslate_iron_ore", 50),

            // Mid-Tier Ores (Utility & Decorative)
            new ConfigBlocks("minecraft", "lapis_ore", 60),
            new ConfigBlocks("minecraft", "deepslate_lapis_ore", 70),
            new ConfigBlocks("minecraft", "redstone_ore", 60),
            new ConfigBlocks("minecraft", "deepslate_redstone_ore", 70),
            new ConfigBlocks("minecraft", "gold_ore", 90),
            new ConfigBlocks("minecraft", "deepslate_gold_ore", 100),

            // Rare & Heavy Ores (High Tier)
            new ConfigBlocks("minecraft", "emerald_ore", 180),
            new ConfigBlocks("minecraft", "deepslate_emerald_ore", 200),
            new ConfigBlocks("minecraft", "diamond_ore", 300),
            new ConfigBlocks("minecraft", "deepslate_diamond_ore", 300),

            // Nether & End Materials
            new ConfigBlocks("minecraft", "nether_quartz_ore", 20),
            new ConfigBlocks("minecraft", "nether_gold_ore", 15),
            new ConfigBlocks("minecraft", "ancient_debris", 600), // Rarity ceiling
            new ConfigBlocks("minecraft", "netherrack", 2),
            new ConfigBlocks("minecraft", "end_stone", 5),

            // Nether Specialty Blocks
            new ConfigBlocks("minecraft", "gilded_blackstone", 120),
            new ConfigBlocks("minecraft", "nether_gold_ore", 15),
            new ConfigBlocks("minecraft", "magma_block", 10),
            new ConfigBlocks("minecraft", "glowstone", 15),

            // Volcanic & Nether Construction
            new ConfigBlocks("minecraft", "basalt", 5),
            new ConfigBlocks("minecraft", "smooth_basalt", 5),
            new ConfigBlocks("minecraft", "blackstone", 4),
            new ConfigBlocks("minecraft", "gilded_blackstone", 120),

            // Aquatic Materials
            new ConfigBlocks("minecraft", "prismarine", 15),
            new ConfigBlocks("minecraft", "prismarine_bricks", 20),
            new ConfigBlocks("minecraft", "dark_prismarine", 25),
            new ConfigBlocks("minecraft", "sea_lantern", 30),

            // Decorative / Industrial
            new ConfigBlocks("minecraft", "calcite", 4),
            new ConfigBlocks("minecraft", "dripstone_block", 6),

            // Special Case: Obsidian
            // Higher than stone due to hardness 50.0 vs 1.5
            new ConfigBlocks("minecraft", "obsidian", 120),
            new ConfigBlocks("minecraft", "crying_obsidian", 150)


    );
}