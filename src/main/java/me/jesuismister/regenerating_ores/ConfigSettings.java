package me.jesuismister.regenerating_ores;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ConfigSettings(
        boolean showParticles,
        boolean disablePushing,
        boolean disableTransitions,
        boolean verboseLogging
) {
    public static final Codec<ConfigSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.BOOL.fieldOf("show_particles").forGetter(ConfigSettings::showParticles),
        Codec.BOOL.fieldOf("disable_pushing").forGetter(ConfigSettings::disablePushing),
        Codec.BOOL.fieldOf("disable_transitions").forGetter(ConfigSettings::disableTransitions),
        Codec.BOOL.fieldOf("verbose_logging").forGetter(ConfigSettings::verboseLogging)
    ).apply(inst, ConfigSettings::new));

    public static final ConfigSettings DEFAULT = new ConfigSettings(
        true,
        true,
        true,
        false
    );

}
