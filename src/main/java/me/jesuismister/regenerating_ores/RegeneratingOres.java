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
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
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
    private static final Logger LOGGER = LogUtils.getLogger();

    public RegeneratingOres(IEventBus modEventBus, ModContainer modContainer) {
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);

        modEventBus.addListener(this::setupDynamicPack);
    }

    private void setupDynamicPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        PackLocationInfo locInfo = new PackLocationInfo("dynamic_res", Component.literal("Dynamic"), PackSource.BUILT_IN, Optional.empty());

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

        Pack pack = Pack.readMetaAndCreate(
                locInfo,
                supplier,
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );

        if (pack != null) {
            event.addRepositorySource(repository -> repository.accept(pack));
        }
    }

    private static class MyPack extends AbstractPackResources {
        private final Map<ResourceLocation, String> files = new HashMap<>();

        public MyPack(PackLocationInfo info) {
            super(info);

            // TODO - process a json config that defines these instead
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "coal_ore", 10, true, false));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "copper_ore", 20, true, false));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "diamond_ore", 300, false, true));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "emerald_ore", 60, false, true));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "gold_ore", 30, false, true));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "iron_ore", 20, true, false));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "lapis_ore", 30, true, false));
            ModBlocks.supportedBlocks.add(new Regenerable("minecraft", "redstone_ore", 30, false, true));

            addPickaxeSupport();

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "data/minecraft/tags/block/needs_iron_tool.json"),
                "{\n" +
                "  \"replace\": false,\n" +
                "  \"values\": [\n" +
                "    \"regenerating_ores:regenerating_diamond_ore\",\n" +
                "    \"regenerating_ores:regenerating_emerald_ore\",\n" +
                "    \"regenerating_ores:regenerating_gold_ore\",\n" +
                "    \"regenerating_ores:regenerating_redstone_ore\"\n" +
                "  ]\n" +
                "}"
            );

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "data/minecraft/tags/block/needs_stone_tool.json"),
                "{\n" +
                "  \"replace\": false,\n" +
                "  \"values\": [\n" +
                "    \"regenerating_ores:regenerating_coal_ore\",\n" +
                "    \"regenerating_ores:regenerating_copper_ore\",\n" +
                "    \"regenerating_ores:regenerating_iron_ore\",\n" +
                "    \"regenerating_ores:regenerating_lapis_ore\"\n" +
                "  ]\n" +
                "}"
            );

            addLanguageSupport();

            for (Regenerable block : ModBlocks.supportedBlocks) {
                addRegeneratingAesthetic(block.namespace, block.blockName);
            }

            for (Regenerable block : ModBlocks.supportedBlocks) {
                addMirroredLootTable(block.namespace, block.blockName);
            }

            for (Regenerable block : ModBlocks.supportedBlocks) {
                ModBlocks.registerBlock("regenerating_" + block.blockName, RegeneratingOreBlock::new);
            }

        }

        public void addLanguageSupport(){
            String working = "{\n";
            for (Regenerable block : ModBlocks.supportedBlocks) {
                working += "  \"block.regenerating_ores.regenerating_" + block.blockName + "\": \"Regenerating " + block.GetCleanName() + "\",\n";
            }
            working += "  \"creativetab.regenerating_ores.regenerating_ores_tab_name\": \"Regenerating Ores\"\n}\n";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "resources/assets/regenerating_ores/lang/en_us.json"), working);
        }

        public void addPickaxeSupport(){
            String working = "{\n \"values\": [\n";
            for (Regenerable block : ModBlocks.supportedBlocks) {
                working += "    \"regenerating_ores:regenerating_" + block.blockName + "\",\n";
            }
            working += "  ]\n}";
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "data/minecraft/tags/block/mineable/pickaxe.json"), working);
        }

        public void addRegeneratingAesthetic(String namespace, String blockName)
        {
            // determines what the block looks like while regenerating,
            // TODO - should probably be config file driven
            final String REPLACE_BLOCK = "minecraft:block/bedrock";

            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "resources/assets/regenerating_ores/blockstates/regenerating_" + blockName + ".json"),
                "{\n" +
                "  \"variants\": {\n" +
                "    \"regenerating=false\": { \"model\": \"" + namespace + ":block/" + blockName + "\" },\n" +
                "    \"regenerating=true\": { \"model\": \"" + REPLACE_BLOCK + "\" }\n" +
                "  }\n" +
                "}"
            );
        }

        public void addMirroredLootTable(String namespace, String blockName)
        {
            files.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, "resources/data/regenerating_ores/loot_table/blocks/regenerating_" + blockName + ".json"),
                "{\n" +
                "  \"type\": \"minecraft:block\",\n" +
                "  \"pools\": [\n" +
                "    {\n" +
                "      \"rolls\": 1,\n" +
                "      \"entries\": [\n" +
                "        {\n" +
                "          \"type\": \"minecraft:loot_table\",\n" +
                "          \"name\": \"" + namespace + ":blocks/" + blockName + "\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}"
            );
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType t, ResourceLocation l) {
            String s = files.get(l);
            return s == null ? null : () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        }

        @Override // Required to satisfy the interface in 1.21.1
        public IoSupplier<InputStream> getRootResource(String... paths) {
            return null;
        }

        @Override
        public void listResources(PackType t, String ns, String p, ResourceOutput out) {
            files.forEach((loc, s) -> { if (loc.getNamespace().equals(ns) && loc.getPath().startsWith(p)) out.accept(loc, getResource(t, loc)); });
        }

        @Override public Set<String> getNamespaces(PackType t) { return Set.of(MOD_ID, "minecraft"); }
        @Override public void close() { files.clear(); }
    }
}