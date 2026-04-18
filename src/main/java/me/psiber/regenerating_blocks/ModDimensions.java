package me.psiber.regenerating_blocks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

public class ModDimensions {
    public static final ResourceKey<Level> MIRROR_OVERWORLD = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "mirror_overworld")
    );
    public static final ResourceKey<Level> MIRROR_NETHER = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "mirror_the_nether")
    );
    public static final ResourceKey<Level> MIRROR_THE_END = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "mirror_the_end")
    );

    static boolean isMirrorDimension(ResourceKey<Level> key) {
        return key == MIRROR_OVERWORLD ||
                key == MIRROR_NETHER ||
                key == MIRROR_THE_END;
    }

    public static ResourceKey<Level> getMirrorKey(ResourceKey<Level> source) {
        if (source == Level.OVERWORLD) return MIRROR_OVERWORLD;
        if (source == Level.NETHER) return MIRROR_NETHER;
        if (source == Level.END) return MIRROR_THE_END;
        return null;
    }
}
