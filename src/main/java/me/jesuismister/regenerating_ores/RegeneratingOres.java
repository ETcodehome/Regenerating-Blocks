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

        // TODO - process a json config that defines these instead

        // use map to allow for dict lookup by block type Map<String, Regenerable> supportedBlocks = new HashMap<>();
        ModBlocks.supportedBlocks = new ArrayList();
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "coal_ore", 10, true, false));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "copper_ore", 20, true, false));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "diamond_ore", 300, false, true));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "emerald_ore", 60, false, true));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "gold_ore", 30, false, true));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "iron_ore", 20, true, false));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "lapis_ore", 30, true, false));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "redstone_ore", 30, false, true));
        ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "stone", 5, false, false));

        ModBlocks.blockMap = new HashMap<String, String>();
        ModBlocks.blockMap.put("regenerating_ores:regenerating_coal_ore","minecraft:coal_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_copper_ore","minecraft:copper_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_iron_ore","minecraft:iron_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_diamond_ore","minecraft:diamond_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_emerald_ore","minecraft:emerald_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_redstone_ore","minecraft:redstone_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_lapis_ore","minecraft:lapis_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_gold_ore","minecraft:gold_ore");
        ModBlocks.blockMap.put("regenerating_ores:regenerating_stone","minecraft:stone");

        // append virtual resources
        modEventBus.addListener(this::setupDynamicPack);

        // ready the deferred blocks
        for (Regenerable block : ModBlocks.supportedBlocks) {
            block.deferredBlock = ModBlocks.registerBlock("regenerating_" + block.blockName, RegeneratingOreBlock::new);
        }

        // do registration
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);

        // load config stuff
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }

    private void setupDynamicPack(AddPackFindersEvent event) {

        PackLocationInfo locInfo = new PackLocationInfo("regenerating_ores", Component.literal("Regenerating Ores Resources"), PackSource.BUILT_IN, Optional.empty());

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

            addPickaxeSpecializations();
            addLanguageSupport();
            addInventoryAesthetic();

            for (Regenerable block : ModBlocks.supportedBlocks) {
                addRegeneratingAesthetic(block.namespace, block.blockName);
            }

        }

        public void addInventoryAesthetic()
        {
            for (Regenerable block : ModBlocks.supportedBlocks) {
                files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/item/regenerating_" + block.blockName + ".json"),
                    "{\n" +
                    " \"parent\": \"" + block.namespace + ":block/" + block.blockName +"\"\n" +
                    "}"
                );
            }
        }

        public void addLanguageSupport()
        {
            String working = "{\n";
            for (Regenerable block : ModBlocks.supportedBlocks) {
                working += "  \"block.regenerating_ores.regenerating_" + block.blockName + "\": \"Regenerating " + block.GetCleanName() + "\",\n";
            }
            working += "  \"creativetab.regenerating_ores.regenerating_ores_tab_name\": \"Regenerating Ores\"\n}\n";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "lang/en_us.json"), working);
        }

        public void addPickaxeSpecializations(){

            // make picks the tool used for breaking regenerable blocks
            String proxyTag = """
                    {"values": [
                    """;
            for (Regenerable block : ModBlocks.supportedBlocks) {
                    proxyTag += "\"regenerating_ores:regenerating_" + block.blockName + "\",";
            }

            if (proxyTag.endsWith(",")) {
                proxyTag = proxyTag.substring(0, proxyTag.length() - 1); // trim the trailing comma
            }

            proxyTag += "]}";

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "tags/block/mineable/pickaxe.json"), proxyTag);

            // stone picks
            String workingStone = "{\n" +
                    "  \"replace\": false,\n" +
                    "  \"values\": [";
            for (Regenerable block : ModBlocks.supportedBlocks) {
                if (block.needStonePick && !block.needIronPick) {
                    workingStone += "\"regenerating_ores:regenerating_" + block.blockName + "\",";
                }
            }

            if (workingStone.endsWith(",")) {
                workingStone = workingStone.substring(0, workingStone.length() - 1); // trim the trailing comma
            }

            workingStone += "]\n}";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "tags/block/needs_stone_tool.json"), workingStone);

            // iron picks
            String workingIron = "{\n" +
                    "  \"replace\": false,\n" +
                    "  \"values\": [";
            for (Regenerable block : ModBlocks.supportedBlocks) {
                if (block.needIronPick) {
                    workingIron += "\"regenerating_ores:regenerating_" + block.blockName + "\",";
                }
            }

            if (workingIron.endsWith(",")) {
                workingIron = workingIron.substring(0, workingIron.length() - 1); // trim the trailing comma
            }

            workingIron += "]\n}";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "tags/block/needs_iron_tool.json"), workingIron);
        }

        public void addRegeneratingAesthetic(String namespace, String blockName)
        {
            // determines what the block looks like while regenerating,
            final String REPLACE_BLOCK = "minecraft:block/bedrock";

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "blockstates/regenerating_" + blockName + ".json"),
                "{\n" +
                "  \"variants\": {\n" +
                "    \"regenerating=false\": { \"model\": \"" + namespace + ":block/" + blockName + "\" },\n" +
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