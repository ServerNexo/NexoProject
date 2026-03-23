package me.nexo.dungeons.matchmaking;

import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueManager {

    private final NexoDungeons plugin;
    // Cola de jugadores esperando usando una lista FIFO (Primero en entrar, primero en salir)
    private final LinkedList<UUID> waveQueue = new LinkedList<>();

    // 🗺️ Aquí registramos nuestras arenas estáticas (En un entorno real, esto se lee de un config.yml)
    // Para el ejemplo, hemos "hardcodeado" dos arenas en el mundo principal.
    private final Map<String, Location> configuredArenas;

    public QueueManager(NexoDungeons plugin) {
        this.plugin = plugin;
        this.configuredArenas = Map.of(
                "Arena_Coliseo", new Location(Bukkit.getWorlds().get(0), 1000, 64, 1000),
                "Arena_Infernal", new Location(Bukkit.getWorlds().get(0), -1000, 64, -1000)
        );
        iniciarMotorDeEmparejamiento();
    }

    // 📥 Añadir a la cola
    public void addPlayerToWaves(Player p) {
        if (waveQueue.contains(p.getUniqueId())) {
            p.sendMessage("§cYa estás en la cola de emparejamiento.");
            return;
        }
        waveQueue.add(p.getUniqueId());
        p.sendMessage("§a⚔ ¡Has entrado a la cola para las Arenas de Supervivencia!");
        p.sendMessage("§7Posición actual en la fila: §e#" + waveQueue.size());
    }

    // 📤 Quitar de la cola (Si se desconecta o cancela)
    public void removePlayer(Player p) {
        waveQueue.remove(p.getUniqueId());
    }

    // ⚙️ El bucle que revisa constantemente
    private void iniciarMotorDeEmparejamiento() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (waveQueue.isEmpty()) return;

            // Buscamos si hay alguna arena que no esté activa en el WaveManager
            for (Map.Entry<String, Location> entry : configuredArenas.entrySet()) {
                String arenaId = entry.getKey();

                if (!plugin.getWaveManager().isArenaActive(arenaId)) {

                    // ¡Arena Libre! Sacamos hasta 3 jugadores de la cola para formar un escuadrón
                    List<Player> escuadron = new ArrayList<>();
                    while (escuadron.size() < 3 && !waveQueue.isEmpty()) {
                        UUID playerId = waveQueue.poll();
                        if (playerId == null) continue;

                        Player p = Bukkit.getPlayer(playerId);
                        if (p != null && p.isOnline()) {
                            escuadron.add(p);
                        }
                    }

                    if (escuadron.isEmpty()) continue; // Falsa alarma, se desconectaron todos

                    // Teletransportamos al escuadrón y arrancamos la partida
                    for (Player p : escuadron) {
                        p.teleport(entry.getValue());
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        p.sendMessage("§e§l¡PARTIDA ENCONTRADA! §7Arena: §f" + arenaId);
                        p.sendMessage("§7Aliados en el escuadrón: §a" + escuadron.size());
                    }

                    // Le decimos al WaveManager que encienda esta arena
                    plugin.getWaveManager().startArena(arenaId, entry.getValue());

                    if (waveQueue.isEmpty()) break; // Si ya no hay fila, dejamos de buscar
                }
            }
        }, 20L, 40L); // Revisa cada 2 segundos (40 Ticks)
    }
}