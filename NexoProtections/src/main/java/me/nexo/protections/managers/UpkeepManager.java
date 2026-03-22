package me.nexo.protections.managers;

import me.nexo.core.NexoCore;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpkeepManager {

    private final ClaimManager claimManager;
    private final NexoCore core;

    public UpkeepManager(NexoProtections plugin, ClaimManager claimManager) {
        this.claimManager = claimManager;
        this.core = NexoCore.getPlugin(NexoCore.class);
        startEnergyDrainTask(plugin);
    }

    private void startEnergyDrainTask(NexoProtections plugin) {
        // 🌟 Tarea en Hilo Secundario cada 10 minutos (12000 Ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (claimManager.getAllStones().isEmpty()) return;

            try (Connection conn = core.getDatabaseManager().getConnection()) {
                // Usamos Batch Update: Enviamos todas las piedras juntas a Supabase en 1 solo viaje
                String sql = "UPDATE nexo_protections SET current_energy = ? WHERE stone_id = CAST(? AS UUID)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {

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

                    ps.executeBatch(); // ¡Enviamos cientos/miles de updates de golpe!
                    plugin.getLogger().info("🔋 NexoProtections: Mantenimiento procesado. Energía actualizada.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error actualizando la energía de las protecciones: " + e.getMessage());
            }
        }, 12000L, 12000L);
    }
}