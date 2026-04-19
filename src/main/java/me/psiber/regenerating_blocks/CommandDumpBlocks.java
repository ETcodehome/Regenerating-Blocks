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
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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

                    JsonArray configArray = new JsonArray();

                    for (Block block : BuiltInRegistries.BLOCK) {
                        BlockState state = block.defaultBlockState();
                        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                        String namespace = key.getNamespace();

                        boolean isFull = state.isCollisionShapeFullBlock(source.getLevel(), pos);
                        boolean isNotEntity = !(block instanceof EntityBlock);
                        boolean isStatic = !(block instanceof FallingBlock);
                        boolean isNotOrganic = !(state.is(BlockTags.LOGS)
                                || state.is(BlockTags.LEAVES)
                                || state.is(BlockTags.MINEABLE_WITH_AXE));
                        boolean isOpaque = state.canOcclude();
                        boolean isNotWool = !(state.is(BlockTags.WOOL));
                        boolean isNotInfested = !(block instanceof InfestedBlock);
                        boolean isNotBricks = !(state.is(BlockTags.STONE_BRICKS));
                        String blockPath = BuiltInRegistries.BLOCK.getKey(block).getPath();
                        boolean isNotStonecut = !(blockPath.contains("polished")
                                || blockPath.contains("brick")
                                || blockPath.contains("cut")
                                || blockPath.contains("carved")
                                || blockPath.contains("chiseled")
                                || blockPath.contains("smooth"));
                        boolean isNotWaterloggable = !(block instanceof SimpleWaterloggedBlock);
                        boolean isNotReactive = !(block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                                || block instanceof net.minecraft.world.level.block.DispenserBlock
                                || block instanceof net.minecraft.world.level.block.TntBlock
                                || block instanceof net.minecraft.world.level.block.ObserverBlock
                                || blockPath.contains("lamp")
                                || blockPath.contains("note_block"));
                        boolean isNotSponge = !(block instanceof SpongeBlock || block instanceof WetSpongeBlock);
                        boolean isNotGlazed = !(block instanceof GlazedTerracottaBlock);
                        boolean isNotTarget = !(block instanceof TargetBlock);
                        boolean isNotFroglight = !(blockPath.contains("froglight"));
                        boolean isNotUtility = !(block instanceof net.minecraft.world.level.block.RespawnAnchorBlock
                                || blockPath.contains("lodestone"));
                        boolean isNotLightLogic = !(blockPath.contains("bulb") || blockPath.contains("shroomlight"));
                        boolean isNotIndestructible = !(block instanceof net.minecraft.world.level.block.EndPortalFrameBlock
                                || state.is(BlockTags.FEATURES_CANNOT_REPLACE)
                                || state.is(BlockTags.DRAGON_IMMUNE)
                                || state.is(BlockTags.WITHER_IMMUNE));
                        boolean isNotCopycat = !(blockPath.contains("copycat"));
                        boolean isNotWaxed = !(blockPath.contains("waxed"));
                        boolean isNotHarvested = !(blockPath.contains("wart_block")
                                || blockPath.contains("hay_block")
                                || blockPath.contains("honeycomb_block")
                                || blockPath.contains("dried_kelp_block")
                                );
                        boolean isNotRawStorage = !(state.is(TagKey.create(Registries.BLOCK,
                                ResourceLocation.fromNamespaceAndPath("c", "storage_blocks"))));

                        if (isFull
                                && isNotWaxed
                                && isNotRawStorage
                                && isNotHarvested
                                && isNotCopycat
                                && isNotIndestructible
                                && isNotLightLogic
                                && isNotUtility
                                && isNotFroglight
                                && isNotTarget
                                && isNotGlazed
                                && isNotSponge
                                && isNotEntity
                                && isStatic
                                && isNotOrganic
                                && isOpaque
                                && isNotInfested
                                && isNotStonecut
                                && isNotBricks
                                && isNotWaterloggable
                                && isNotReactive
                                && isNotWool)
                        {

                            JsonObject blockEntry = new JsonObject();
                            blockEntry.addProperty("namespace", namespace);
                            blockEntry.addProperty("block_name", blockPath);

                            // Logic-based default regen time
                            blockEntry.addProperty("regen_after", calculateDefaultRegen(block, state, blockPath));

                            configArray.add(blockEntry);
                        }
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
