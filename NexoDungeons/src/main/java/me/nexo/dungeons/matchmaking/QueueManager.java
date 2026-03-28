package me.nexo.dungeons.matchmaking;

import me.nexo.core.crossplay.CrossplayUtils;
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
    private final LinkedList<UUID> waveQueue = new LinkedList<>();
    private final Map<String, Location> configuredArenas;

    public QueueManager(NexoDungeons plugin) {
        this.plugin = plugin;
        this.configuredArenas = Map.of(
                "Sector_Coliseo", new Location(Bukkit.getWorlds().get(0), 1000, 64, 1000),
                "Sector_Infernal", new Location(Bukkit.getWorlds().get(0), -1000, 64, -1000)
        );
        iniciarMotorDeEmparejamiento();
    }

    public void addPlayerToWaves(Player p) {
        if (waveQueue.contains(p.getUniqueId())) {
            CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.queue.ya-en-cola"));
            return;
        }
        waveQueue.add(p.getUniqueId());
        CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.queue.unido-cola"));
        CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.queue.posicion-cola").replace("%pos%", String.valueOf(waveQueue.size())));
    }

    public void removePlayer(Player p) {
        waveQueue.remove(p.getUniqueId());
    }

    private void iniciarMotorDeEmparejamiento() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (waveQueue.isEmpty()) return;

            for (Map.Entry<String, Location> entry : configuredArenas.entrySet()) {
                String arenaId = entry.getKey();

                if (!plugin.getWaveManager().isArenaActive(arenaId)) {

                    List<Player> escuadron = new ArrayList<>();
                    while (escuadron.size() < 3 && !waveQueue.isEmpty()) {
                        UUID playerId = waveQueue.poll();
                        if (playerId == null) continue;

                        Player p = Bukkit.getPlayer(playerId);
                        if (p != null && p.isOnline()) {
                            escuadron.add(p);
                        }
                    }

                    if (escuadron.isEmpty()) continue;

                    for (Player p : escuadron) {
                        p.teleport(entry.getValue());
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.queue.emparejamiento-exitoso").replace("%arena%", arenaId));
                        CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.queue.tamano-escuadron").replace("%size%", String.valueOf(escuadron.size())));
                    }

                    plugin.getWaveManager().startArena(arenaId, entry.getValue());

                    if (waveQueue.isEmpty()) break;
                }
            }
        }, 20L, 40L);
    }
}