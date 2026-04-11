package me.jesuismister.regenerating_ores;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigManager {
    private static final Path BLOCKS_FILE = FMLPaths.CONFIGDIR.get().resolve("regenerating_ores.json");
    private static final Path SETTINGS_FILE = FMLPaths.CONFIGDIR.get().resolve("regenerating_ores_settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<ConfigBlocks> blocks = ConfigBlocks.DEFAULT;
    private static ConfigSettings settings = ConfigSettings.DEFAULT;

    public static void load() {
        loadBlocks();
        loadSettings();
    }

    private static void loadBlocks(){

        if (!Files.exists(BLOCKS_FILE)) {
            System.out.println("No block config found, generating defaults...");
            saveBlocks();
        }

        try (var reader = Files.newBufferedReader(BLOCKS_FILE)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);
            blocks = ConfigBlocks.CODEC.listOf().parse(JsonOps.INSTANCE, json)
                    .getOrThrow(error -> new RuntimeException("Blocks Error: " + error));
            System.out.println("Successfully loaded " + blocks.size() + " regenerable blocks.");
        } catch (Exception e) {
            System.err.println("Failed to load block config, using defaults. Error: " + e.getMessage());
        }

    }

    private static void loadSettings(){

        if (!Files.exists(SETTINGS_FILE)) {
            System.out.println("No settings config found, generating defaults...");
            saveSettings();
        }

        try (var reader = Files.newBufferedReader(SETTINGS_FILE)) {
            var json = GSON.fromJson(reader, com.google.gson.JsonElement.class);
            settings = ConfigSettings.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(s -> new RuntimeException("Settings Error: " + s));
            System.out.println("Successfully loaded Regenerating Ores config settings.");
        } catch (Exception e) {
            System.err.println("Failed to load settings, using defaults.");
        }

    }

    private static void saveBlocks() {
        try {
            JsonElement json = ConfigBlocks.CODEC.listOf().encodeStart(JsonOps.INSTANCE, blocks)
                    .getOrThrow(error -> new RuntimeException("Save Error: " + error));
            Files.writeString(BLOCKS_FILE, GSON.toJson(json));
        } catch (Exception e) {
            System.err.println("Could not save block config: " + e.getMessage());
        }
    }

    private static void saveSettings() {

        try {
            var json = ConfigSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                    .getOrThrow(error -> new RuntimeException("Save Error: " + error));
            Files.writeString(SETTINGS_FILE, GSON.toJson(json));
        } catch (Exception e) {
            System.err.println("Could not save settings config: " + e.getMessage());
        }
    }

    public static List<ConfigBlocks> getSupportedBlocks() {
        return blocks;
    }

    public static ConfigSettings getSettings() {
        return settings;
    }
}