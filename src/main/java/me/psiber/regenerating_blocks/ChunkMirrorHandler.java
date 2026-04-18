package me.psiber.regenerating_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ChunkMirrorHandler {

    // Session cache to track which chunks have the flag on disk.
    // Uses Long encoded ChunkPos for memory efficiency.
    private static final Set<Long> ALREADY_MIRRORED = ConcurrentHashMap.newKeySet();

    /**
     * Step 1: Scan the incoming NBT from disk/generator.
     */
    @SubscribeEvent
    public static void onDataLoad(ChunkDataEvent.Load event) {
        if (event.getData().getBoolean("RegenMirrored")) {
            ALREADY_MIRRORED.add(event.getChunk().getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onDataSave(ChunkDataEvent.Save event) {
        long posLong = event.getChunk().getPos().toLong();
        // If we've mirrored this chunk during this session,
        // make sure the "Mirrored" flag is saved to the disk NBT.
        if (ALREADY_MIRRORED.contains(posLong)) {
            event.getData().putBoolean("RegenMirrored", true);
        }
    }

    /**
     * Step 2: Act once the chunk is physically in the level.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        mirrorPristineChunk(event.getChunk());
    }

    public static void mirrorPristineChunk(ChunkAccess chunk) {
        Level level = chunk.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel sourceLevel)) {
            return;
        }

        // Explicitly ignore chunks that are part of the mirror dimensions.
        // This prevents the mirror from populating the session cache for itself.
        ResourceKey<Level> sourceKey = sourceLevel.dimension();
        if (isMirrorDimension(sourceKey)) {
            return;
        }

        ChunkPos pos = chunk.getPos();
        long posLong = pos.toLong();

        // Session check (fast)
        if (ALREADY_MIRRORED.contains(posLong)) {
            // Noisy RegeneratingBlock.log("Chunk mirrored already: " + chunk);
            return;
        }

        // 1. Only process FULL chunks in dimensions we care about
        if (!chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
            RegeneratingBlocks.log("FAILED MIRRORING OF CHUNK (NOT READY) chunk: " + chunk);
            return;
        }

        ResourceKey<Level> targetKey = getMirrorKey(sourceLevel.dimension());
        if (targetKey == null) return;

        // 3. Perform Mirroring Logic
        ServerLevel mirrorLevel = sourceLevel.getServer().getLevel(targetKey);
        if (mirrorLevel == null) return;

        // Persisted check (once off slow)
        boolean alreadyInMirror = mirrorLevel.getChunkSource().chunkMap.read(pos).join()
                .map(nbt -> nbt.getBoolean("RegenMirrored"))
                .orElse(false);

        if (alreadyInMirror) {
            RegeneratingBlocks.log("Mirror chunk already exists: " + pos + " to " + targetKey.location());
            ALREADY_MIRRORED.add(posLong);
            return;
        }

        RegeneratingBlocks.log("Mirroring pristine chunk: " + pos + " to " + targetKey.location());

        // Create NBT from the current state (which at this moment is freshly generated/loaded)
        CompoundTag mirrorData = ChunkSerializer.write(sourceLevel, chunk);

        // Strip entities/block entities to keep it as a terrain-only snapshot
        mirrorData.remove("entities");
        mirrorData.remove("block_entities");
        mirrorData.remove("PostProcessing");

        // Set the flag for both the Mirror and the Overworld
        mirrorData.putBoolean("RegenMirrored", true);

        // Injects into Mirror storage
        mirrorLevel.getChunkSource().chunkMap.write(pos, mirrorData);

        // Mark as mirrored so that we don't repeat this for the rest of the session
        ALREADY_MIRRORED.add(posLong);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            ALREADY_MIRRORED.remove(event.getChunk().getPos().toLong());
        }
    }

    private static boolean isMirrorDimension(ResourceKey<Level> key) {
        return key == ModDimensions.MIRROR_OVERWORLD ||
                key == ModDimensions.MIRROR_NETHER ||
                key == ModDimensions.MIRROR_THE_END;
    }

    public static ResourceKey<Level> getMirrorKey(ResourceKey<Level> source) {
        if (source == Level.OVERWORLD) return ModDimensions.MIRROR_OVERWORLD;
        if (source == Level.NETHER) return ModDimensions.MIRROR_NETHER;
        if (source == Level.END) return ModDimensions.MIRROR_THE_END;
        return null;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld == null) return;

        RegeneratingBlocks.log("Starting Spawn Chunk Mirror Sweep...");

        // Spawn chunks in 1.21.1 are typically within a small radius of the world spawn
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        int radius = overworld.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS);

        // Convert to chunk coordinates
        int spawnX = spawnPos.getX() >> 4;
        int spawnZ = spawnPos.getZ() >> 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos pos = new ChunkPos(spawnX + x, spawnZ + z);
                LevelChunk chunk = overworld.getChunkSource().getChunk(pos.x, pos.z, false);

                if (chunk != null && !ALREADY_MIRRORED.contains(pos.toLong())) {
                    if (chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                        RegeneratingBlocks.log("Sweep: Capturing spawn chunk at " + pos);

                        mirrorPristineChunk(chunk);
                    }
                }
            }
        }
        RegeneratingBlocks.log("Spawn Sweep Complete.");
    }
}