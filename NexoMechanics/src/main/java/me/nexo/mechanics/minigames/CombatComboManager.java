package me.nexo.mechanics.minigames;

import me.nexo.mechanics.NexoMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatComboManager implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos el plugin de Mechanics
    private final NexoMechanics plugin;

    private final Map<UUID, Integer> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoKill = new ConcurrentHashMap<>();
    public final Map<UUID, Long> enFrenesi = new ConcurrentHashMap<>(); // Usado para que la Energía cueste 0

    public CombatComboManager(NexoMechanics plugin) {
        this.plugin = plugin;
        iniciarDecadenciaCombos();
    }

    @EventHandler
    public void alMatar(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player p = event.getEntity().getKiller();
            UUID id = p.getUniqueId();
            long ahora = System.currentTimeMillis();

            int comboActual = combos.getOrDefault(id, 0) + 1;
            combos.put(id, comboActual);
            ultimoKill.put(id, ahora);

            // Efecto visual/sonoro según el combo
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f + (comboActual * 0.1f));
            p.sendActionBar("§c§l⚔ ¡COMBO x" + comboActual + "!");

            // Activar Frenesí al llegar a 10
            if (comboActual == 10) {
                activarFrenesi(p);
                combos.remove(id); // Reiniciamos el medidor después de activarlo
            }
        }
    }

    private void activarFrenesi(Player p) {
        UUID id = p.getUniqueId();
        enFrenesi.put(id, System.currentTimeMillis() + 10000L); // 10 segundos

        p.sendTitle("§4§l¡FRENESÍ!", "§cEnergía infinita y Velocidad", 5, 40, 5);
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false, true));

        // Magia de Paper: WorldBorder falso para poner la pantalla roja
        WorldBorder bordeRojo = Bukkit.createWorldBorder();
        bordeRojo.setCenter(p.getLocation());
        bordeRojo.setSize(20000000); // Gigante para que no lo encierre
        bordeRojo.setWarningDistance(20000000); // Triggerea el tinte rojo
        p.setWorldBorder(bordeRojo);

        // Apagar frenesí después de 10s
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                p.setWorldBorder(null); // Quita la pantalla roja
                p.sendMessage("§8El Frenesí se ha desvanecido...");
                enFrenesi.remove(id); // 🟢 FIX: Limpiamos el mapa al terminar
            }
        }, 200L);
    }

    private void iniciarDecadenciaCombos() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (UUID id : combos.keySet()) {
                // Si pasaron 3 segundos sin matar, pierde el combo
                if (ahora - ultimoKill.getOrDefault(id, 0L) > 3000) {
                    combos.remove(id);
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) p.sendActionBar("§8Combo perdido...");
                }
            }
        }, 10L, 10L);
    }
}