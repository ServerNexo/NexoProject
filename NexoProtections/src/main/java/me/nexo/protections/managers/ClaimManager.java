package me.nexo.protections.managers;

import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    // Grid Espacial O(1): Llave = Chunk Key ("mundo_x_z"), Valor = Lista de Piedras que tocan ese chunk
    private final Map<String, List<ProtectionStone>> spatialGrid = new ConcurrentHashMap<>();

    // Búsqueda directa por ID (para comandos o integraciones)
    private final Map<UUID, ProtectionStone> stonesById = new ConcurrentHashMap<>();

    // 🌟 Registra una piedra en la memoria RAM
    public void addStoneToCache(ProtectionStone stone) {
        stonesById.put(stone.getStoneId(), stone);

        // Mapear la piedra a todos los chunks que intersecta (Bitwise >> 4 es una forma ultra rápida de dividir entre 16)
        int minChunkX = stone.getBox().minX() >> 4;
        int maxChunkX = stone.getBox().maxX() >> 4;
        int minChunkZ = stone.getBox().minZ() >> 4;
        int maxChunkZ = stone.getBox().maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = stone.getBox().world() + "_" + cx + "_" + cz;

                // Si el chunk no existe en el mapa, crea la lista y añade la piedra
                spatialGrid.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(stone);
            }
        }
    }

    // 🌟 Elimina una piedra de la memoria RAM
    public void removeStoneFromCache(ProtectionStone stone) {
        stonesById.remove(stone.getStoneId());

        int minChunkX = stone.getBox().minX() >> 4;
        int maxChunkX = stone.getBox().maxX() >> 4;
        int minChunkZ = stone.getBox().minZ() >> 4;
        int maxChunkZ = stone.getBox().maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = stone.getBox().world() + "_" + cx + "_" + cz;
                List<ProtectionStone> stonesInChunk = spatialGrid.get(chunkKey);
                if (stonesInChunk != null) {
                    stonesInChunk.remove(stone);
                    if (stonesInChunk.isEmpty()) {
                        spatialGrid.remove(chunkKey);
                    }
                }
            }
        }
    }

    // ⚡ Búsqueda ultra rápida para los Listeners (Cuando un jugador rompe/pone un bloque)
    public ProtectionStone getStoneAt(Location loc) {
        // Obtenemos la llave del chunk donde está el jugador actualmente
        String chunkKey = loc.getWorld().getName() + "_" + (loc.getBlockX() >> 4) + "_" + (loc.getBlockZ() >> 4);

        // Sacamos solo las protecciones de ese chunk (usualmente 0, 1 o 2 máximo)
        List<ProtectionStone> stonesInChunk = spatialGrid.get(chunkKey);

        if (stonesInChunk != null) {
            for (ProtectionStone stone : stonesInChunk) {
                // Verificación exacta a nivel de bloque
                if (stone.getBox().contains(loc)) {
                    return stone;
                }
            }
        }
        return null; // No hay ninguna protección en esa ubicación exacta
    }

    public ProtectionStone getStoneById(UUID id) {
        return stonesById.get(id);
    }

    public Map<UUID, ProtectionStone> getAllStones() {
        return stonesById;
    }
}