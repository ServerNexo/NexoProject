package me.nexo.dungeons.matchmaking;

import me.nexo.core.utils.NexoColor;
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

    // 🎨 PALETA HEX
    private static final String MSG_ALREADY_QUEUED = "&#ff4b2b[!] Ya te encuentras en la cola de emparejamiento.";
    private static final String MSG_JOINED_QUEUE = "&#a8ff78[✓] Has ingresado a la cola del Simulador de Supervivencia.";
    private static final String MSG_QUEUE_POS = "&#434343Posición estimada en la red: &#fbd72b#%pos%";
    private static final String MSG_MATCH_FOUND = "&#fbd72b<bold>¡EMPAREJAMIENTO EXITOSO!</bold> &#434343Sector: &#00fbff%arena%";
    private static final String MSG_SQUAD_SIZE = "&#434343Operarios en el escuadrón: &#a8ff78%size%";

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
            p.sendMessage(NexoColor.parse(MSG_ALREADY_QUEUED));
            return;
        }
        waveQueue.add(p.getUniqueId());
        p.sendMessage(NexoColor.parse(MSG_JOINED_QUEUE));
        p.sendMessage(NexoColor.parse(MSG_QUEUE_POS.replace("%pos%", String.valueOf(waveQueue.size()))));
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
                        p.sendMessage(NexoColor.parse(MSG_MATCH_FOUND.replace("%arena%", arenaId)));
                        p.sendMessage(NexoColor.parse(MSG_SQUAD_SIZE.replace("%size%", String.valueOf(escuadron.size()))));
                    }

                    plugin.getWaveManager().startArena(arenaId, entry.getValue());

                    if (waveQueue.isEmpty()) break;
                }
            }
        }, 20L, 40L);
    }
}