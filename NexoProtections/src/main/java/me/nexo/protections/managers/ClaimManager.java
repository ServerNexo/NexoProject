package me.nexo.protections.managers;

import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    private final Map<String, List<ProtectionStone>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<UUID, ProtectionStone> stonesById = new ConcurrentHashMap<>();

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
                        if (stone.getBox().intersects(newBox)) return true;
                    }
                }
            }
        }
        return false;
    }

    public void addStoneToCache(ProtectionStone stone) {
        stonesById.put(stone.getStoneId(), stone);
        int minChunkX = stone.getBox().minX() >> 4;
        int maxChunkX = stone.getBox().maxX() >> 4;
        int minChunkZ = stone.getBox().minZ() >> 4;
        int maxChunkZ = stone.getBox().maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = stone.getBox().world() + "_" + cx + "_" + cz;
                spatialGrid.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(stone);
            }
        }
    }

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
                    if (stonesInChunk.isEmpty()) spatialGrid.remove(chunkKey);
                }
            }
        }
    }

    public ProtectionStone getStoneAt(Location loc) {
        String chunkKey = loc.getWorld().getName() + "_" + (loc.getBlockX() >> 4) + "_" + (loc.getBlockZ() >> 4);
        List<ProtectionStone> stonesInChunk = spatialGrid.get(chunkKey);
        if (stonesInChunk != null) {
            for (ProtectionStone stone : stonesInChunk) {
                if (stone.getBox().contains(loc)) return stone;
            }
        }
        return null;
    }

    public ProtectionStone getStoneById(UUID id) { return stonesById.get(id); }
    public Map<UUID, ProtectionStone> getAllStones() { return stonesById; }

    // =========================================================================
    // 🌟 NUEVO: GUARDAR MIEMBROS Y LEYES ASÍNCRONAMENTE EN SUPABASE
    // =========================================================================
    public void saveStoneDataAsync(ProtectionStone stone) {
        me.nexo.core.NexoCore core = me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class);
        java.util.concurrent.CompletableFuture.runAsync(() -> {

            // Convertimos la lista de amigos a texto: "uuid1,uuid2,"
            StringBuilder membersStr = new StringBuilder();
            for (UUID uuid : stone.getTrustedFriends()) {
                membersStr.append(uuid.toString()).append(",");
            }

            // Convertimos las flags a texto: "pvp:false,interact:true,"
            StringBuilder flagsStr = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : stone.getFlags().entrySet()) {
                flagsStr.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }

            String sql = "UPDATE nexo_protections SET members = ?, flags = ? WHERE stone_id = CAST(? AS UUID)";
            try (java.sql.Connection conn = core.getDatabaseManager().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, membersStr.toString());
                ps.setString(2, flagsStr.toString());
                ps.setString(3, stone.getStoneId().toString());
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // =========================================================================
    // 🌟 ACTUALIZADO: CARGAR DESDE SUPABASE
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

                    stone.drainEnergy(100000);
                    stone.setMaxEnergy(rs.getDouble("max_energy"));
                    stone.addEnergy(rs.getDouble("current_energy"));

                    // 🌑 CARGAR ACÓLITOS
                    String membersStr = rs.getString("members");
                    if (membersStr != null && !membersStr.isEmpty()) {
                        for (String uuidStr : membersStr.split(",")) {
                            if (!uuidStr.isEmpty()) stone.addFriend(UUID.fromString(uuidStr));
                        }
                    }

                    // 🌑 CARGAR LEYES DEL DOMINIO
                    String flagsStr = rs.getString("flags");
                    if (flagsStr != null && !flagsStr.isEmpty()) {
                        for (String flagData : flagsStr.split(",")) {
                            if (!flagData.isEmpty() && flagData.contains(":")) {
                                String[] parts = flagData.split(":");
                                stone.setFlag(parts[0], Boolean.parseBoolean(parts[1]));
                            }
                        }
                    }

                    addStoneToCache(stone);
                    loaded++;
                }
                org.bukkit.Bukkit.getLogger().info("🛡️ NexoProtections: Se cargaron " + loaded + " zonas protegidas (Con Acólitos y Leyes).");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}