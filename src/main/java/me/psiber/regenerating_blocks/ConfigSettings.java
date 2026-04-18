package me.psiber.regenerating_blocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ConfigSettings(
        boolean showParticles,
        boolean verboseLogging
) {
    public static final Codec<ConfigSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.BOOL.fieldOf("show_particles").forGetter(ConfigSettings::showParticles),
        Codec.BOOL.fieldOf("verbose_logging").forGetter(ConfigSettings::verboseLogging)
    ).apply(inst, ConfigSettings::new));

    public static final ConfigSettings DEFAULT = new ConfigSettings(
        true,
        false
    );

}
