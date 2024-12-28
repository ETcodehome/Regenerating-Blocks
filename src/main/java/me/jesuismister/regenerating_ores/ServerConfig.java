package me.jesuismister.regenerating_ores;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> COAL;
    public static final ModConfigSpec.ConfigValue<Integer> COPPER;
    public static final ModConfigSpec.ConfigValue<Integer> DIAMOND;
    public static final ModConfigSpec.ConfigValue<Integer> EMERALD;
    public static final ModConfigSpec.ConfigValue<Integer> GOLD;
    public static final ModConfigSpec.ConfigValue<Integer> IRON;
    public static final ModConfigSpec.ConfigValue<Integer> LAPIS;
    public static final ModConfigSpec.ConfigValue<Integer> REDSTONE;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Configuration for Regenerating Ores").push("general");

        COAL = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("coalDelay", 10, 1, 3600);
        COPPER = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("copperDelay", 20, 1, 3600);
        DIAMOND = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("diamondDelay", 300, 1, 3600);
        EMERALD = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("emeraldDelay", 60, 1, 3600);
        GOLD = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("goldDelay", 30, 1, 3600);
        IRON = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("ironDelay", 20, 1, 3600);
        LAPIS = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("lapisDelay", 30, 1, 3600);
        REDSTONE = builder
                .comment("Time in seconds before iron regenerate.")
                .defineInRange("redstoneDelay", 30, 1, 3600);

        builder.pop();
        CONFIG_SPEC = builder.build();
    }

    public static int getRegenerationDelay(String oreType) {
        return switch (oreType.toLowerCase()) {
            case "coal" -> COAL.get();
            case "copper" -> COPPER.get();
            case "diamond" -> DIAMOND.get();
            case "emerald" -> EMERALD.get();
            case "gold" -> GOLD.get();
            case "iron" -> IRON.get();
            case "lapis" -> LAPIS.get();
            case "redstone" -> REDSTONE.get();
            default -> 1;
        };
    }

}
