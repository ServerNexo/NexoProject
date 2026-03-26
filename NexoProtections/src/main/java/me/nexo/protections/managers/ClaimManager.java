package me.nexo.protections.managers;

import me.nexo.protections.core.ClaimBox; // 🌟 IMPORT AÑADIDO
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

    // 🌟 NUEVO: Radar de Solapamiento (Evita que pongan piedras muy juntas)
    public boolean hasOverlappingClaim(ClaimBox newBox) {
        int minChunkX = newBox.minX() >> 4;
        int maxChunkX = newBox.maxX() >> 4;
        int minChunkZ = newBox.minZ() >> 4;
        int maxChunkZ = newBox.maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = newBox.world() + "_" + cx + "_" + cz;
                List<ProtectionStone> stonesInChunk = spatialGrid.get(chunkKey);

                if (stonesInChunk != null) {
                    for (ProtectionStone stone : stonesInChunk) {
                        // Si choca con CUALQUIER piedra en este sector, abortamos
                        if (stone.getBox().intersects(newBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false; // Zona libre, se puede aterrizar el escudo
    }

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

    // =========================================================================
    // 🌟 NUEVO MÉTODO: Cargar todas las piedras desde Supabase a la RAM
    // =========================================================================
    public void loadAllStonesAsync(me.nexo.core.NexoCore core) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String sql = "SELECT * FROM nexo_protections";
            try (java.sql.Connection conn = core.getDatabaseManager().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = ps.executeQuery()) {

                int loaded = 0;
                while(rs.next()) {
                    UUID stoneId = UUID.fromString(rs.getString("stone_id"));
                    UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                    String clanStr = rs.getString("clan_id");
                    UUID clanId = (clanStr != null && !clanStr.isEmpty()) ? UUID.fromString(clanStr) : null;

                    String world = rs.getString("world_name");
                    int minX = rs.getInt("min_x");
                    int minY = rs.getInt("min_y");
                    int minZ = rs.getInt("min_z");
                    int maxX = rs.getInt("max_x");
                    int maxY = rs.getInt("max_y");
                    int maxZ = rs.getInt("max_z");

                    me.nexo.protections.core.ClaimBox box = new me.nexo.protections.core.ClaimBox(world, minX, minY, minZ, maxX, maxY, maxZ);
                    ProtectionStone stone = new ProtectionStone(stoneId, ownerId, clanId, box);

                    // Seteamos la energía guardada
                    double currentEnergy = rs.getDouble("current_energy");
                    double maxEnergy = rs.getDouble("max_energy");

                    stone.drainEnergy(100000); // Vaciamos temporalmente para inyectar el valor exacto de la BD
                    stone.setMaxEnergy(maxEnergy);
                    stone.addEnergy(currentEnergy);

                    addStoneToCache(stone);
                    loaded++;
                }
                org.bukkit.Bukkit.getLogger().info("🛡️ NexoProtections: Se cargaron " + loaded + " zonas protegidas en RAM.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}