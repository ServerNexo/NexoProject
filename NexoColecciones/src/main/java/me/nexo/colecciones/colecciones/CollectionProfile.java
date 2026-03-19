package me.nexo.colecciones.colecciones;

// 🟢 ARQUITECTURA: Importamos tu nuevo plugin
import me.nexo.colecciones.NexoColecciones;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionProfile {
    private final UUID playerUUID;
    private final ConcurrentHashMap<String, Integer> progress;
    private boolean needsFlush = false;

    public CollectionProfile(UUID playerUUID, ConcurrentHashMap<String, Integer> loadedProgress) {
        this.playerUUID = playerUUID;
        this.progress = loadedProgress != null ? loadedProgress : new ConcurrentHashMap<>();
    }

    public void addProgress(String id, int amount, boolean isSlayer) {
        int oldAmount = progress.getOrDefault(id, 0);
        int newAmount = oldAmount + amount;
        progress.put(id, newAmount);
        this.needsFlush = true;

        verificarMetas(id, oldAmount, newAmount, isSlayer);
    }

    private void verificarMetas(String id, int oldAmt, int newAmt, boolean isSlayer) {
        // 🟢 ARQUITECTURA: Llamamos a la clase principal de NexoColecciones
        NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

        ConfigurationSection datos = isSlayer ? plugin.getColeccionesConfig().getDatosSlayer(id) : plugin.getColeccionesConfig().getDatosColeccion(id);
        if (datos == null) return;

        ConfigurationSection metas = datos.getConfigurationSection("metas");
        if (metas == null) return;

        for (String nivelStr : metas.getKeys(false)) {
            int metaRequerida = metas.getInt(nivelStr);
            if (oldAmt < metaRequerida && newAmt >= metaRequerida) {
                otorgarRecompensa(datos, nivelStr);
            }
        }
    }

    private void otorgarRecompensa(ConfigurationSection datos, String nivelStr) {
        String nombreBonito = datos.getString("nombre_bonito", "Desconocido").replace("&", "§");
        ConfigurationSection recompensa = datos.getConfigurationSection("recompensas." + nivelStr);
        if (recompensa == null) return;

        // 🟢 ARQUITECTURA: La tarea se lanza a nombre de NexoColecciones
        Bukkit.getScheduler().runTask(NexoColecciones.getPlugin(NexoColecciones.class), () -> {
            org.bukkit.entity.Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage("§a§l¡NIVEL ALCANZADO! §fLlegaste al nivel §e" + nivelStr + " §fen §b" + nombreBonito);

                // Ejecutar comandos de la configuración (Ej: dar dinero o XP)
                List<String> comandos = recompensa.getStringList("comandos");
                for (String cmd : comandos) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        });
    }

    public boolean isNeedsFlush() { return needsFlush; }
    public void setNeedsFlush(boolean needsFlush) { this.needsFlush = needsFlush; }
    public ConcurrentHashMap<String, Integer> getProgress() { return progress; }
    public UUID getPlayerUUID() { return playerUUID; }
}