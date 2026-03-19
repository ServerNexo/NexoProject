package me.nexo.pvp.pvp; // (O me.nexo.pvp dependiendo de tu estructura de carpetas)

// 🟢 ARQUITECTURA: Ahora importamos la clase principal de nuestro nuevo plugin NexoPvP
import me.nexo.pvp.NexoPvP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {

    // 🟢 ARQUITECTURA: Instanciamos NexoPvP en lugar de Main
    private final NexoPvP plugin;

    public final Set<UUID> pvpActivo = ConcurrentHashMap.newKeySet();
    public final Map<UUID, Long> enCombate = new ConcurrentHashMap<>();

    // 🏆 SISTEMA DE HONOR Y BOUNTY (Prueba en RAM)
    public final Map<UUID, Integer> puntosHonor = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> rachaAsesinatos = new ConcurrentHashMap<>();

    // 🟢 ARQUITECTURA: Pedimos NexoPvP en el constructor
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
            p.sendMessage("§c¡No puedes desactivar el Modo de Guerra mientras estás en combate!");
            return;
        }

        if (pvpActivo.contains(id)) {
            pvpActivo.remove(id);
            p.sendMessage("§a🕊️ Modo de Guerra: DESACTIVADO. Estás a salvo.");
        } else {
            pvpActivo.add(id);
            p.sendMessage("§c⚔ Modo de Guerra: ACTIVADO. ¡Lucha por tu honor!");
        }
    }

    public void marcarEnCombate(Player p1, Player p2) {
        long expiracion = System.currentTimeMillis() + 15000L;
        if (!estaEnCombate(p1)) p1.sendMessage("§c§l¡ESTÁS EN COMBATE! §7(15s)");
        if (!estaEnCombate(p2)) p2.sendMessage("§c§l¡ESTÁS EN COMBATE! §7(15s)");
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
                    if (p != null) p.sendMessage("§a✅ Ya no estás en combate.");
                }
            }
        }, 20L, 20L);
    }
}