package me.jesuismister.regenerating_ores;

import me.jesuismister.regenerating_ores.blocks.ModBlocks;
import me.jesuismister.regenerating_ores.blocks.RegeneratingOreBlock;
import me.jesuismister.regenerating_ores.items.ModCreativeModeTabs;
import me.jesuismister.regenerating_ores.items.ModItems;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod(RegeneratingOres.MOD_ID)
public class RegeneratingOres {
    public static final String MOD_ID = "regenerating_ores";

    public RegeneratingOres(IEventBus modEventBus, ModContainer modContainer) {

        List<Regenerable> addTheseBlocksFromConfig = List.of(
            new Regenerable("minecraft", "copper_ore", 20),
            new Regenerable("minecraft", "diamond_ore", 300),
            new Regenerable("minecraft", "emerald_ore", 60),
            new Regenerable("minecraft", "gold_ore", 30),
            new Regenerable("minecraft", "iron_ore", 20),
            new Regenerable("minecraft", "lapis_ore", 30),
            new Regenerable("minecraft", "redstone_ore", 30),
            new Regenerable("minecraft", "stone", 5),
            new Regenerable("minecraft", "obsidian", 5)
        );

        // Populate a lookup table so we can get all configuration data from blockstates
        ModBlocks.supportedBlocks = new HashMap<String, Regenerable>();
        for (Regenerable block : addTheseBlocksFromConfig )
        {
            ModBlocks.supportedBlocks.put(block.GetRegeneratingNameWithNamespace(), block);
        }

        // append virtual resources
        modEventBus.addListener(this::setupDynamicPack);

        // ready the deferred blocks
        for (Regenerable block : ModBlocks.supportedBlocks.values()) {
            block.deferredBlock = ModBlocks.registerBlock(block.GetRegeneratingBlockName(), () -> new RegeneratingOreBlock(block));
        }

        // do registration
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);

        // load config stuff
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }

    private void setupDynamicPack(AddPackFindersEvent event) {

        PackLocationInfo locInfo = new PackLocationInfo(MOD_ID, Component.literal("Regenerating Ores Resources"), PackSource.BUILT_IN, Optional.empty());

        // Explicitly define the ResourcesSupplier to avoid functional interface ambiguity
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo packLocationInfo) {
                return new MyPack(packLocationInfo);
            }

            @Override
            public PackResources openFull(PackLocationInfo packLocationInfo, Pack.Metadata metadata) {
                return new MyPack(packLocationInfo);
            }
        };

        // handle asset subpaths
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Pack clientPack = Pack.readMetaAndCreate(
                    locInfo,
                    supplier,
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );

            if (clientPack != null) {
                event.addRepositorySource(repository -> repository.accept(clientPack));
            }
        }

        // handle data subpaths
        if (event.getPackType() == PackType.SERVER_DATA) {
            Pack serverPack = Pack.readMetaAndCreate(
                    locInfo,
                    supplier,
                    PackType.SERVER_DATA,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );

            if (serverPack != null) {
                event.addRepositorySource(repository -> repository.accept(serverPack));
            }
        }

    }

    private static class MyPack extends AbstractPackResources {
        private final Map<ResourceLocation, String> files = new HashMap<>();

        public MyPack(PackLocationInfo info) {
            super(info);

            addLanguageSupport();
            addInventoryAesthetic();

            for (Regenerable block : ModBlocks.supportedBlocks.values()) {
                addRegeneratingAesthetic(block);
            }

        }

        public void addInventoryAesthetic()
        {
            for (Regenerable block : ModBlocks.supportedBlocks.values()) {
                files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/item/" + block.GetRegeneratingBlockName() + ".json"),
                    "{\n" +
                    " \"parent\": \"" + block.namespace + ":block/" + block.blockName +"\"\n" +
                    "}"
                );
            }
        }

        public void addLanguageSupport()
        {
            String working = "{\n";
            for (Regenerable block : ModBlocks.supportedBlocks.values()) {
                working += "  \"block.regenerating_ores." + block.GetRegeneratingBlockName() + "\": \"" + block.GetPresentationName() + "\",\n";
            }
            working += "  \"creativetab.regenerating_ores.regenerating_ores_tab_name\": \"Regenerating Ores\"\n}\n";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "lang/en_us.json"), working);
        }

        public void addRegeneratingAesthetic(Regenerable block)
        {
            // determines what the block looks like while regenerating,
            final String REPLACE_BLOCK = "minecraft:block/bedrock";

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "blockstates/" + block.GetRegeneratingBlockName() + ".json"),
                "{\n" +
                "  \"variants\": {\n" +
                "    \"regenerating=false\": { \"model\": \"" + block.namespace + ":block/" + block.blockName + "\" },\n" +
                "    \"regenerating=true\": { \"model\": \"" + REPLACE_BLOCK + "\" }\n" +
                "  }\n" +
                "}"
            );
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {

            String content = files.get(location);
            if (content == null) return null;

            return () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... paths) {

            // we return the pack metadata manually here since the pack is dynamic
            if (paths[0] == "pack.mcmeta") {
                String metadata = """
                {
                  "pack": {
                    "description": "Regenerating Ores Resources",
                    "pack_format": 34
                  }
                }
                """;
                return () -> new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8));
            }

            return null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {

            files.keySet().forEach(location -> {
                if (location.getPath().startsWith(path)) {
                    output.accept(location, this.getResource(type, location));
                }
            });
        }

        @Override public Set<String> getNamespaces(PackType t) { return Set.of(MOD_ID, "minecraft"); }
        @Override public void close() { files.clear(); }
    }
}