package me.psiber.regenerating_blocks;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.loading.FMLPaths;
import com.google.gson.JsonArray;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommandDumpBlocks {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDumpBlocks.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("suggestblocks")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    Path outputPath = FMLPaths.GAMEDIR.get().resolve("suggested_blocks.txt");
                    BlockPos pos = BlockPos.containing(source.getPosition());

                    Path processingPath = FMLPaths.GAMEDIR.get().resolve("processing_blocks.txt");
                    List<String> processingResults = new ArrayList<>();

                    JsonArray configArray = new JsonArray();

                    for (Block block : BuiltInRegistries.BLOCK) {

                        BlockState state = block.defaultBlockState();
                        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                        String namespace = key.getNamespace();
                        String blockPath = key.getPath();

                        boolean isNotFullBlock = !state.isCollisionShapeFullBlock(source.getLevel(), pos);
                        if(isNotFullBlock){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isNotFullBlock");
                            continue;
                        }

                        boolean isEntity = block instanceof EntityBlock;
                        if(isEntity){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isEntity");
                            continue;
                        }

                        boolean isFallingBlock = block instanceof FallingBlock;
                        if(isFallingBlock){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isFallingBlock");
                            continue;
                        }

                        boolean isLogsOrLeaves = (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES));
                        if(isLogsOrLeaves){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isLogsOrLeaves");
                            continue;
                        }

                        boolean isMineableWithAxe = state.is(BlockTags.MINEABLE_WITH_AXE);
                        if(isMineableWithAxe){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isMineableWithAxe");
                            continue;
                        }

                        boolean isTransparent = !state.canOcclude();
                        if(isTransparent){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isTransparent");
                            continue;
                        }

                        boolean isWool = state.is(BlockTags.WOOL);
                        if(isWool){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isWool");
                            continue;
                        }

                        boolean isInfested = block instanceof InfestedBlock;
                        if(isInfested){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isInfested");
                            continue;
                        }

                        boolean isBricks = state.is(BlockTags.STONE_BRICKS);
                        if(isBricks){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isBricks");
                            continue;
                        }

                        boolean isStonecut = blockPath.contains("polished")
                                || blockPath.contains("brick")
                                || blockPath.contains("cut")
                                || blockPath.contains("carved")
                                || blockPath.contains("chiseled")
                                || blockPath.contains("smooth");
                        if(isStonecut){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isStonecut");
                            continue;
                        }

                        boolean isWaterloggable = block instanceof SimpleWaterloggedBlock;
                        if(isWaterloggable){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isWaterloggable");
                            continue;
                        }

                        boolean isReactive = block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                                || block instanceof net.minecraft.world.level.block.DispenserBlock
                                || block instanceof net.minecraft.world.level.block.TntBlock
                                || block instanceof net.minecraft.world.level.block.ObserverBlock
                                || blockPath.contains("lamp")
                                || blockPath.contains("note_block");
                        if(isReactive){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isReactive");
                            continue;
                        }

                        boolean isSponge = block instanceof SpongeBlock || block instanceof WetSpongeBlock;
                        if(isSponge){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isSponge");
                            continue;
                        }

                        boolean isGlazed = block instanceof GlazedTerracottaBlock;
                        if(isGlazed){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isGlazed");
                            continue;
                        }

                        boolean isTarget = block instanceof TargetBlock;
                        if(isTarget){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isTarget");
                            continue;
                        }

                        boolean isFroglight = blockPath.contains("froglight");
                        if(isFroglight){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isFroglight");
                            continue;
                        }

                        boolean isUtility = block instanceof net.minecraft.world.level.block.RespawnAnchorBlock
                                || blockPath.contains("lodestone");
                        if(isUtility){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isUtility");
                            continue;
                        }

                        boolean isBulb = blockPath.contains("bulb") || blockPath.contains("shroomlight");
                        if(isBulb){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isBulb");
                            continue;
                        }

                        boolean isFunctionallyIndestructible = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 0;
                        if (isFunctionallyIndestructible) {
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isFunctionallyIndestructible");
                            continue;
                        }

                        boolean isCopycat = blockPath.contains("copycat");
                        if(isCopycat){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isCopycat");
                            continue;
                        }

                        boolean isWaxed = blockPath.contains("waxed");
                        if(isWaxed){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isWaxed");
                            continue;
                        }

                        boolean isHarvested = blockPath.contains("wart_block")
                                || blockPath.contains("hay_block")
                                || blockPath.contains("honeycomb_block")
                                || blockPath.contains("dried_kelp_block");
                        if(isHarvested){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isHarvested");
                            continue;
                        }

                        boolean isRawStorage = state.is(TagKey.create(Registries.BLOCK,
                                ResourceLocation.fromNamespaceAndPath("c", "storage_blocks")));
                        if(isRawStorage){
                            processingResults.add("  " + String.format("%-50s", namespace + ":" + blockPath) + " isRawStorage");
                            continue;
                        }

                        // block is probably a good regenerate candidate
                        JsonObject blockEntry = new JsonObject();
                        blockEntry.addProperty("namespace", namespace);
                        blockEntry.addProperty("block_name", blockPath);
                        blockEntry.addProperty("regen_after", calculateDefaultRegen(block, state, blockPath));
                        configArray.add(blockEntry);

                        processingResults.add("✓ " + namespace + ":" + blockPath);
                    }

                    // Pretty-print the JSON
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String jsonOutput = gson.toJson(configArray);

                    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                        writer.write(jsonOutput);
                        source.sendSuccess(() -> Component.literal("Dumped " + configArray.size() + " blocks to " + outputPath.getFileName()), true);
                    } catch (IOException e) {
                        source.sendFailure(Component.literal("Failed to write file: " + e.getMessage()));
                    }

                    // Write the rejected file
                    try (FileWriter writer = new FileWriter(processingPath.toFile())) {
                        for (String line : processingResults) {
                            writer.write(line + System.lineSeparator());
                        }
                    } catch (IOException e) {
                        source.sendFailure(Component.literal("Failed to write processing file." + e.getMessage()));
                    }

                    return 1;
                })
        );
    }

    private static int calculateDefaultRegen(Block block, BlockState state, String name) {
        float resistance = block.getExplosionResistance();
        int baseTime;

        // 1. Determine base time by Resistance Tiers
        if (resistance <= 1.0f) baseTime = 5;        // Netherrack, Dirt
        else if (resistance <= 6.0f) baseTime = 10;   // Stone, Granite, Andesite
        else if (resistance <= 10.0f) baseTime = 20;  // Standard Ores (Coal, Iron)
        else if (resistance <= 30.0f) baseTime = 60;  // Metal Blocks, Deepslate Ores
        else if (resistance <= 1200.0f) baseTime = 150; // Obsidian / Crying Obsidian
        else baseTime = 20;

        // 2. Specialized Overrides
        if (name.equals("ancient_debris")) return 600;

        // 3. Check if the block is an Ore
        // Tags include minecraft:coal_ores, iron_ores, copper_ores, etc.
        boolean isOre = state.is(TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("c", "ores")));

        // 4. Apply the 4x Multiplier for Ores
        return isOre ? baseTime * 4 : baseTime;
    }
}
