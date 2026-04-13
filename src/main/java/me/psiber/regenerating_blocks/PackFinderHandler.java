package me.psiber.regenerating_blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackSelectionConfig;

import java.util.Optional;

public class PackFinderHandler {

    public static void register(AddPackFindersEvent event) {

        PackLocationInfo locInfo = new PackLocationInfo(RegeneratingBlocks.MOD_ID, Component.literal("Regenerating Blocks Resources"), PackSource.BUILT_IN, Optional.empty());

        // Explicitly define the ResourcesSupplier to avoid functional interface ambiguity
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo packLocationInfo) {
                return new DynamicResourcePack(packLocationInfo);
            }

            @Override
            public PackResources openFull(PackLocationInfo packLocationInfo, Pack.Metadata metadata) {
                return new DynamicResourcePack(packLocationInfo);
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
}
