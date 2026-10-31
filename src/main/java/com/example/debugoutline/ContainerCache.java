package com.example.debugoutline;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Cache des conteneurs, indexé par chunk.
 *
 * La liste ne dépend ni de la position ni de l'orientation du joueur : une entrée reste
 * tant que son chunk est chargé. Elle n'est retirée que lors du déchargement du chunk
 * (ou lorsqu'on quitte le monde). C'est ce qui corrige la disparition des conteneurs
 * quand on se retourne.
 */
public final class ContainerCache {

    private static final Map<ChunkPos, Map<BlockPos, ContainerType>> BY_CHUNK = new HashMap<>();

    private ContainerCache() {}

    /** Parcourt les chunks chargés dans un carré de rayon `radius` autour de `center`. */
    public static void rescan(ClientWorld world, BlockPos center, int radius) {
        int minCx = (center.getX() - radius) >> 4;
        int maxCx = (center.getX() + radius) >> 4;
        int minCz = (center.getZ() - radius) >> 4;
        int maxCz = (center.getZ() + radius) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk != null) {
                    scanChunk(chunk);
                }
            }
        }
    }

    private static void scanChunk(WorldChunk chunk) {
        Map<BlockPos, ContainerType> found = new HashMap<>();
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            ContainerType type = ContainerType.of(entry.getValue());
            if (type != null) {
                found.put(entry.getKey().toImmutable(), type);
            }
        }
        BY_CHUNK.put(chunk.getPos(), found);
    }

    public static void unload(ChunkPos pos) {
        BY_CHUNK.remove(pos);
    }

    public static void clear() {
        BY_CHUNK.clear();
    }

    public static int size() {
        int n = 0;
        for (Map<BlockPos, ContainerType> m : BY_CHUNK.values()) {
            n += m.size();
        }
        return n;
    }

    public static void forEach(BiConsumer<BlockPos, ContainerType> action) {
        for (Map<BlockPos, ContainerType> m : BY_CHUNK.values()) {
            for (Map.Entry<BlockPos, ContainerType> e : m.entrySet()) {
                action.accept(e.getKey(), e.getValue());
            }
        }
    }
}
