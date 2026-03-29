package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.NexoCore;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class FlushTask extends BukkitRunnable {

    private final NexoColecciones plugin;
    private final Gson gson = new Gson();

    // 🌟 AHORA SÍ: El constructor acepta tu plugin perfectamente
    public FlushTask(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String sql = "INSERT INTO nexo_collections (uuid, collections_data, claimed_tiers) VALUES (?, ?::jsonb, ?::jsonb) " +
                "ON CONFLICT (uuid) DO UPDATE SET collections_data = EXCLUDED.collections_data, claimed_tiers = EXCLUDED.claimed_tiers";

        // 🌟 MAGIA: Pedimos la conexión prestada directamente a NexoCore
        try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCount = 0;

            // Toma los perfiles directo del Cerebro
            for (CollectionProfile profile : plugin.getCollectionManager().getPerfiles().values()) {
                if (profile.isNeedsFlush()) {
                    ps.setString(1, profile.getPlayerUUID().toString());

                    // Empaquetamos en JSON el progreso base y los tiers reclamados
                    ps.setString(2, gson.toJson(profile.getProgressMap()));
                    ps.setString(3, gson.toJson(profile.getClaimedTiersMap()));

                    ps.addBatch();

                    profile.setNeedsFlush(false);
                    batchCount++;
                }
            }

            if (batchCount > 0) {
                ps.executeBatch();
                conn.commit();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al guardar datos de colecciones en Supabase.");
            e.printStackTrace();
        }
    }
}