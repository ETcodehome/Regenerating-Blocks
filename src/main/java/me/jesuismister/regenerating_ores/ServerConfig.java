package me.jesuismister.regenerating_ores;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ServerConfig {
    // Stores the file in /config/regenerating_ores.json
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("regenerating_ores.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // We hold the loaded list here for easy access across the mod
    private static List<RegenerableConfig> loadedConfigs = List.of();

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            System.out.println("No config found, generating defaults...");
            loadedConfigs = createDefaultConfig();
            return;
        }

        try (var reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);

            // The Codec does the heavy lifting: translating JSON to Records
            loadedConfigs = RegenerableConfig.CODEC.listOf().parse(JsonOps.INSTANCE, json)
                    .getOrThrow(error -> new RuntimeException("Configuration Error: " + error));

            System.out.println("Successfully loaded " + loadedConfigs.size() + " regenerable blocks.");
        } catch (Exception e) {
            System.err.println("Failed to load config, using defaults. Error: " + e.getMessage());
            loadedConfigs = createDefaultConfig();
        }
    }

    private static List<RegenerableConfig> createDefaultConfig() {
        List<RegenerableConfig> defaults = List.of(
                new RegenerableConfig("minecraft", "copper_ore", 20),
                new RegenerableConfig("minecraft", "diamond_ore", 300),
                new RegenerableConfig("minecraft", "emerald_ore", 60),
                new RegenerableConfig("minecraft", "gold_ore", 30),
                new RegenerableConfig("minecraft", "iron_ore", 20),
                new RegenerableConfig("minecraft", "lapis_ore", 30),
                new RegenerableConfig("minecraft", "redstone_ore", 30),
                new RegenerableConfig("minecraft", "stone", 5),
                new RegenerableConfig("minecraft", "obsidian", 5)
        );
        save(defaults);
        return defaults;
    }

    public static void save(List<RegenerableConfig> data) {
        try {
            // Encode the Records back into JSON format
            JsonElement json = RegenerableConfig.CODEC.listOf().encodeStart(JsonOps.INSTANCE, data)
                    .getOrThrow(error -> new RuntimeException("Save Error: " + error));

            Files.writeString(CONFIG_FILE, GSON.toJson(json));
        } catch (Exception e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public static List<RegenerableConfig> getLoadedConfigs() {
        return loadedConfigs;
    }
}