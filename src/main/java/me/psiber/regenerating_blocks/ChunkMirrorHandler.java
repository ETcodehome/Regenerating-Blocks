package me.psiber.regenerating_blocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;

@EventBusSubscriber(modid = RegeneratingBlocks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ChunkMirrorHandler {

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {

        // Early exit guard to ensure only finished state is ever processed
        if (!event.getChunk().getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
            return;
        }

        // 1. Identify Source
        if (!(event.getLevel() instanceof ServerLevel sourceLevel)) return;

        // Only mirror the main dimensions
        ResourceKey<Level> sourceKey = sourceLevel.dimension();
        ResourceKey<Level> targetKey = getMirrorKey(sourceKey);
        if (targetKey == null) return;

        // 2. Get the NBT Data Object
        CompoundTag chunkData = event.getData();

        // 3. Deep Copy & Filter
        // We copy it so we don't accidentally modify the Overworld's original save tag
        CompoundTag mirrorData = chunkData.copy();

        // Strip volatile data (Entities/Block Entities) to avoid UUID conflicts
        mirrorData.remove("entities");
        mirrorData.remove("block_entities");
        // We also remove 'structures' so the mirror world remains a terrain-only cache
        //mirrorData.remove("structures");

        // 4. Push to Mirror World Storage
        MinecraftServer server = sourceLevel.getServer();
        ServerLevel mirrorLevel = server.getLevel(targetKey);

        if (mirrorLevel != null) {
            // We use the ChunkSource's data storage to manually 'inject' the NBT
            // This bypasses the Mirror World's own generation entirely
            mirrorLevel.getChunkSource().chunkMap.write(event.getChunk().getPos(), mirrorData);
        }
    }

    public static ResourceKey<Level> getMirrorKey(ResourceKey<Level> source) {
        if (source == Level.OVERWORLD) return ModDimensions.MIRROR_OVERWORLD;
        if (source == Level.NETHER) return ModDimensions.MIRROR_NETHER;
        if (source == Level.END) return ModDimensions.MIRROR_THE_END;
        return null;
    }
}