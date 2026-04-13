package me.psiber.regenerating_blocks;

import me.psiber.regenerating_blocks.blocks.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.PackType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DynamicResourcePack extends AbstractPackResources {
    private final Map<ResourceLocation, String> files = new HashMap<>();

    public DynamicResourcePack(PackLocationInfo info) {
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
            files.put(ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "models/item/" + block.GetRegeneratingBlockName() + ".json"),
                    "{\n" +
                    " \"parent\": \"" + block.namespace + ":block/" + block.blockName +"\"\n" +
                    "}"
            );
        }
    }

    public void addLanguageSupport()
    {
        String working = "{\n";
        working += "  \"text.regenerating_blocks.format\": \"Regenerating %s\",\n";
        working += "  \"creativetab.regenerating_blocks.regenerating_blocks_tab_name\": \"Regenerating Blocks\"\n}\n";
        files.put(ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "lang/en_us.json"), working);
    }

    public void addRegeneratingAesthetic(Regenerable block)
    {
        // determines what the block looks like while regenerating
        String modelLocation = block.namespace + ":block/" + block.blockName;

        files.put(ResourceLocation.fromNamespaceAndPath(RegeneratingBlocks.MOD_ID, "blockstates/" + block.GetRegeneratingBlockName() + ".json"),
                "{\n" +
                "  \"variants\": {\n" +
                "    \"\": { \"model\": \"" + modelLocation + "\" }\n" +
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
                    "description": "Regenerating Blocks Resources",
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

    @Override public Set<String> getNamespaces(PackType t) { return Set.of(RegeneratingBlocks.MOD_ID, "minecraft"); }
    @Override public void close() { files.clear(); }
}
