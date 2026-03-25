package me.nexo.pvp.pvp;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
import me.nexo.pvp.NexoPvP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {

    private final NexoPvP plugin;

    public final Set<UUID> pvpActivo = ConcurrentHashMap.newKeySet();
    public final Map<UUID, Long> enCombate = new ConcurrentHashMap<>();

    // 🏆 SISTEMA DE HONOR Y BOUNTY (Prueba en RAM)
    public final Map<UUID, Integer> puntosHonor = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> rachaAsesinatos = new ConcurrentHashMap<>();

    public PvPManager(NexoPvP plugin) {
        this.plugin = plugin;
        iniciarRelojCombate();
    }

    public boolean tienePvP(Player p) {
        return pvpActivo.contains(p.getUniqueId());
    }

    public void togglePvP(Player p) {
        UUID id = p.getUniqueId();

        if (estaEnCombate(p)) {
            p.sendMessage(NexoColor.parse("&#FF5555[!] Error de Seguridad: No puedes desactivar la hostilidad con un enlace de combate activo."));
            return;
        }

        if (pvpActivo.contains(id)) {
            pvpActivo.remove(id);
            p.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>PROTOCOLO DE PAZ:</bold> &#AAAAAAHostilidad desactivada. Escudos neuronales activos."));
        } else {
            pvpActivo.add(id);
            p.sendMessage(NexoColor.parse("&#FF5555[!] <bold>PROTOCOLO DE GUERRA:</bold> &#AAAAAAHostilidad activada. Sistemas de armamento en línea."));
        }
    }

    public void marcarEnCombate(Player p1, Player p2) {
        long expiracion = System.currentTimeMillis() + 15000L;

        if (!estaEnCombate(p1)) {
            p1.sendMessage(NexoColor.parse("&#FF5555<bold>¡ALERTA DE COMBATE!</bold> &#AAAAAAEnlace táctico detectado (15s). No te desconectes."));
        }
        if (!estaEnCombate(p2)) {
            p2.sendMessage(NexoColor.parse("&#FF5555<bold>¡ALERTA DE COMBATE!</bold> &#AAAAAAEnlace táctico detectado (15s). No te desconectes."));
        }

        enCombate.put(p1.getUniqueId(), expiracion);
        enCombate.put(p2.getUniqueId(), expiracion);
    }

    public boolean estaEnCombate(Player p) {
        return enCombate.containsKey(p.getUniqueId());
    }

    private void iniciarRelojCombate() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : enCombate.entrySet()) {
                if (ahora > entry.getValue()) {
                    enCombate.remove(entry.getKey());
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        p.sendMessage(NexoColor.parse("&#55FF55[✓] Enlace de combate finalizado. Sistemas estabilizados."));
                    }
                }
            }
        }, 20L, 20L);
    }
}