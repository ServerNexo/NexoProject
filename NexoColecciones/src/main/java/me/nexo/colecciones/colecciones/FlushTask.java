package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.colecciones.NexoColecciones;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class FlushTask extends BukkitRunnable {
    private final HikariDataSource hikari;
    private final Gson gson = new Gson();

    public FlushTask(HikariDataSource hikari) {
        this.hikari = hikari;
    }

    @Override
    public void run() {
        String sql = "INSERT INTO nexo_collections (uuid, collections_data) VALUES (?, ?::jsonb) " +
                "ON CONFLICT (uuid) DO UPDATE SET collections_data = EXCLUDED.collections_data";

        try (Connection conn = hikari.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCount = 0;

            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Toma los perfiles directo del Cerebro
            for (CollectionProfile profile : plugin.getCollectionManager().getPerfiles().values()) {
                if (profile.isNeedsFlush()) {
                    ps.setString(1, profile.getPlayerUUID().toString());
                    ps.setString(2, gson.toJson(profile.getProgress()));
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
            e.printStackTrace();
        }
    }
}