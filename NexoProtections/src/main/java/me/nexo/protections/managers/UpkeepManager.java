package me.nexo.protections.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * 🛡️ NexoProtections - Gestor de Mantenimiento (Arquitectura Enterprise)
 */
@Singleton
public class UpkeepManager {

    private final NexoProtections plugin;
    private final ClaimManager claimManager;
    private final NexoCore core;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public UpkeepManager(NexoProtections plugin, ClaimManager claimManager, NexoCore core) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.core = core;
        startEnergyDrainTask();
    }

    private void startEnergyDrainTask() {
        // 🌟 Tarea en Hilo Secundario cada 10 minutos (12000 Ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (claimManager.getAllStones().isEmpty()) return;

            String sql = "UPDATE nexo_protections SET current_energy = ? WHERE stone_id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (ProtectionStone stone : claimManager.getAllStones().values()) {

                    // Lógica de Economía: Clanes gastan 10 de energía, solitarios gastan 2
                    double consumo = (stone.getClanId() != null) ? 10.0 : 2.0;

                    // Restamos la energía en la memoria RAM
                    stone.drainEnergy(consumo);

                    // Preparamos la consulta para el envío masivo
                    ps.setDouble(1, stone.getCurrentEnergy());
                    ps.setString(2, stone.getStoneId().toString());
                    ps.addBatch();
                }

                ps.executeBatch(); // ¡Enviamos cientos/miles de updates de golpe a Supabase!
                plugin.getLogger().info("🔋 NexoProtections: Mantenimiento procesado. Energía actualizada en la BD.");

            } catch (Exception e) {
                plugin.getLogger().severe("Error actualizando la energía de las protecciones: " + e.getMessage());
            }
        }, 12000L, 12000L);
    }
}