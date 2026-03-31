package me.nexo.mechanics.minigames;

import me.nexo.core.utils.NexoColor;
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

    private final NexoMechanics plugin;

    private final Map<UUID, Integer> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoKill = new ConcurrentHashMap<>();
    public final Map<UUID, Long> enFrenesi = new ConcurrentHashMap<>();

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

            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f + (comboActual * 0.1f));
            p.sendActionBar(NexoColor.parse("&#8b0000<bold>⚔ ¡RACHA DE COMBATE x" + comboActual + "!</bold>"));

            if (comboActual == 10) {
                activarFrenesi(p);
                combos.remove(id);
            }
        }
    }

    private void activarFrenesi(Player p) {
        UUID id = p.getUniqueId();
        enFrenesi.put(id, System.currentTimeMillis() + 10000L);

        p.sendTitle(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#8b0000<bold>¡FRENESÍ!</bold>")),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#ff00ffEnergía de Núcleo Infinita")),
                5, 40, 5
        );
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false, true));

        WorldBorder bordeRojo = Bukkit.createWorldBorder();
        bordeRojo.setCenter(p.getLocation());
        bordeRojo.setSize(20000000);
        bordeRojo.setWarningDistance(20000000);
        p.setWorldBorder(bordeRojo);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                p.setWorldBorder(null);
                p.sendMessage(NexoColor.parse("&#E6CCFFEl Frenesí se ha desvanecido. Sistemas retornando a la normalidad."));
                enFrenesi.remove(id);
            }
        }, 200L);
    }

    private void iniciarDecadenciaCombos() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (UUID id : combos.keySet()) {
                if (ahora - ultimoKill.getOrDefault(id, 0L) > 3000) {
                    combos.remove(id);
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) p.sendActionBar(NexoColor.parse("&#E6CCFFRacha de combate finalizada."));
                }
            }
        }, 10L, 10L);
    }
}