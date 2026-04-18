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
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ChunkMirrorHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        mirrorChunkIfRequired(event.getChunk());
    }

    public static void mirrorChunkIfRequired(ChunkAccess chunk) {

        Level level = chunk.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel sourceLevel)) {
            return;
        }

        // Only process FULL chunks in dimensions we care about. Earlier states should be ignored
        if (!chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
            RegeneratingBlocks.log("FAILED MIRRORING OF CHUNK (NOT READY) chunk: " + chunk);
            return;
        }

        // Guard against a chunk load inside a mirror dimension
        ResourceKey<Level> sourceKey = sourceLevel.dimension();
        if (ModDimensions.isMirrorDimension(sourceKey)) {
            // We deliberately don't take any action in a mirror dimension.
            // chunks generated here by players moving around will be void and later overwritten.
            return;
        }

        ResourceKey<Level> targetKey = ModDimensions.getMirrorKey(sourceLevel.dimension());
        if (targetKey == null) {
            RegeneratingBlocks.log("Mirror key was null");
            return;
        }

        ServerLevel mirrorLevel = sourceLevel.getServer().getLevel(targetKey);
        if (mirrorLevel == null) {
            RegeneratingBlocks.log("Mirror Level was null");
            return;
        }

        // Checks the block in the mirror dimension
        ChunkPos pos = chunk.getPos();
        if (isPhysicallyMirrored(mirrorLevel, pos)) {
            RegeneratingBlocks.log("Chunk is already mirrored at " + pos);
            return;
        }

        /////////////////////////////////////////////////////////////////////////////
        // FIRST TIME LOAD HANDLING
        /////////////////////////////////////////////////////////////////////////////

        RegeneratingBlocks.log("First Load. Mirroring pristine chunk: " + pos + " to " + targetKey.location());

        // Create NBT from the current state (which at this moment is freshly generated/loaded)
        CompoundTag mirrorData = ChunkSerializer.write(sourceLevel, chunk);

        // Strip entities/block entities to keep it as a terrain-only snapshot
        mirrorData.remove("entities");
        mirrorData.remove("block_entities");
        mirrorData.remove("PostProcessing");

        // Injects into Mirror storage
        mirrorLevel.getChunkSource().chunkMap.write(pos, mirrorData);

        // Create a temporary ProtoChunk
        RegionStorageInfo storageInfo = new RegionStorageInfo(
                sourceLevel.getServer().getWorldData().getLevelName(),
                targetKey,
                "chunk"
        );

        ProtoChunk proto = ChunkSerializer.read(
                mirrorLevel,
                mirrorLevel.getPoiManager(),
                storageInfo,
                pos,
                mirrorData
        );

        // Hot-swap the block sections into the live Mirror RAM
        LevelChunk mirrorChunk = mirrorLevel.getChunkSource().getChunk(pos.x, pos.z, true);
        if (mirrorChunk != null && proto != null) {

            // Replace the block sections of the
            // empty mirror chunk with the data from the protochunk
            for (int i = 0; i < mirrorChunk.getSections().length; i++) {
                mirrorChunk.getSections()[i] = proto.getSections()[i];
            }

            // Finalise the copy by marking it
            markMirrorAsComplete(mirrorLevel, pos);
            mirrorChunk.setUnsaved(true);
        }
    }

    private static void markMirrorAsComplete(ServerLevel mirrorLevel, ChunkPos pos) {

        // Get the absolute block coordinates for the center of the chunk at the very bottom
        // minY is -64 in 1.21.1 Overworld/Dimensions but varies by dimension
        BlockPos flagPos = new BlockPos(pos.getMinBlockX() + 8, mirrorLevel.getMinBuildHeight(), pos.getMinBlockZ() + 8);

        // We use flag 2 (Send to clients) and 16 (Don't trigger neighbors/physics)
        mirrorLevel.setBlock(flagPos, net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 18);
    }

    private static boolean isPhysicallyMirrored(Level mirrorLevel, ChunkPos pos) {
        BlockPos flagPos = new BlockPos(pos.getMinBlockX() + 8, mirrorLevel.getMinBuildHeight(), pos.getMinBlockZ() + 8);

        // Check the block state at the bottom of the world
        return mirrorLevel.getBlockState(flagPos).is(net.minecraft.world.level.block.Blocks.BARRIER);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {

        // Annoying, spawn chunks do not trigger onChunkLoad reliably.
        // So instead we sweep them here
        MinecraftServer server = event.getServer();
        sweepSpawn(server, Level.OVERWORLD);
        sweepSpawn(server, Level.NETHER);
        sweepSpawn(server, Level.END);
    }

    private static void sweepSpawn(MinecraftServer server, ResourceKey<Level> dimKey) {

        ServerLevel level = server.getLevel(dimKey);
        if (level == null) return;

        BlockPos spawnPos = level.getSharedSpawnPos();
        int radius = level.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS);
        int spawnX = spawnPos.getX() >> 4;
        int spawnZ = spawnPos.getZ() >> 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos pos = new ChunkPos(spawnX + x, spawnZ + z);
                LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
                if (chunk != null && chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                    mirrorChunkIfRequired(chunk);
                }
            }
        }

    }

}

