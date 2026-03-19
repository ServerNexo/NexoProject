package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

// 🟢 ARQUITECTURA: Importamos tu nuevo plugin en lugar del Main del Core
import me.nexo.colecciones.NexoColecciones;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager {
    private static final ConcurrentHashMap<UUID, CollectionProfile> profiles = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    public static CollectionProfile getProfile(UUID uuid) { return profiles.get(uuid); }
    public static void removeProfile(UUID uuid) { profiles.remove(uuid); }
    public static Iterable<CollectionProfile> getAllProfiles() { return profiles.values(); }

    // CARGAR DESDE SUPABASE AL ENTRAR
    public static void loadPlayerFromDatabase(UUID uuid, HikariDataSource hikari) {
        String sql = "SELECT collections_data FROM nexo_collections WHERE uuid = ?";

        // 🟢 ARQUITECTURA: La tarea asíncrona ahora pertenece a NexoColecciones
        Bukkit.getScheduler().runTaskAsynchronously(NexoColecciones.getPlugin(NexoColecciones.class), () -> {
            try (Connection conn = hikari.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                ConcurrentHashMap<String, Integer> progress = new ConcurrentHashMap<>();
                if (rs.next()) {
                    String json = rs.getString("collections_data");
                    Type type = new TypeToken<ConcurrentHashMap<String, Integer>>(){}.getType();
                    progress = gson.fromJson(json, type);
                }

                profiles.put(uuid, new CollectionProfile(uuid, progress));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}