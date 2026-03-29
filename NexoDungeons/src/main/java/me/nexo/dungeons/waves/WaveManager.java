package me.nexo.dungeons.waves;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaveManager implements Listener {

    private final NexoDungeons plugin;
    // Mapeo de Arenas Activas (ArenaID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    public WaveManager(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // Método para iniciar una arena desde un comando o un bloque (JSON)
    public void startArena(String arenaId, Location center) {
        if (activeArenas.containsKey(arenaId)) {
            plugin.getLogger().warning("La arena " + arenaId + " ya está activa.");
            return;
        }

        // 🌟 CORRECCIÓN: Le entregamos la instancia del plugin a la Arena para que pueda ejecutar tareas
        WaveArena arena = new WaveArena(plugin, arenaId, center);
        activeArenas.put(arenaId, arena);
        arena.start();
    }

    public void stopArena(String arenaId) {
        WaveArena arena = activeArenas.remove(arenaId);
        if (arena != null) {
            arena.stop();
        }
    }

    // 🌟 NUEVO: Para que el QueueManager sepa si la arena está ocupada o libre
    public boolean isArenaActive(String arenaId) {
        WaveArena arena = activeArenas.get(arenaId);
        return arena != null && arena.isActive();
    }

    // 💀 LISTENER: Detecta cuando muere un MythicMob
    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        UUID deadMobId = event.getEntity().getUniqueId();

        // Buscamos a qué arena pertenecía este monstruo
        for (WaveArena arena : activeArenas.values()) {
            if (arena.isActive()) {
                arena.registrarMuerteMob(deadMobId);
            }
        }
    }
}