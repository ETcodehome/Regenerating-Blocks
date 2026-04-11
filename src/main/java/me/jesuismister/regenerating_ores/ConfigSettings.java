package me.jesuismister.regenerating_ores;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record ConfigSettings(
        boolean showParticles,
        boolean disablePushing,
        boolean verboseLogging,
        String regeneratingVisual
) {
    public static final Codec<ConfigSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.BOOL.fieldOf("show_particles").forGetter(ConfigSettings::showParticles),
        Codec.BOOL.fieldOf("disable_pushing").forGetter(ConfigSettings::disablePushing),
        Codec.BOOL.fieldOf("verbose_logging").forGetter(ConfigSettings::verboseLogging),
        Codec.STRING.fieldOf("regenerating_visual").forGetter(ConfigSettings::regeneratingVisual)
    ).apply(inst, ConfigSettings::new));

    public static final ConfigSettings DEFAULT = new ConfigSettings(
        true,
        false,
        true,
        "minecraft:bedrock"
    );

    public String getRegeneratingBlockResourcePath(){
        ResourceLocation rl = ResourceLocation.parse(regeneratingVisual);
        return rl.getNamespace() + ":block/" + rl.getPath();
    }

    public static int lightCache = -1;
    public int getRegeneratingBlockLightEmission(){
        if (lightCache != -1){
            return lightCache;
        }
        ResourceLocation rl = ResourceLocation.parse(regeneratingVisual);
        Block block = BuiltInRegistries.BLOCK.get(rl);
        lightCache = block.defaultBlockState().getLightEmission();
        return lightCache;
    }

}
