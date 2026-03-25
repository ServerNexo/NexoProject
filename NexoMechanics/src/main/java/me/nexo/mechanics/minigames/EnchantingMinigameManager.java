package me.nexo.mechanics.minigames;

import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantingMinigameManager implements Listener {

    private final NexoMechanics plugin;

    // UUID del jugador -> Lista de ArmorStands (Runas) en orden que debe golpear
    private final Map<UUID, List<ArmorStand>> runasActivas = new ConcurrentHashMap<>();

    // Set de jugadores que se ganaron un encantamiento gratis
    public final Set<UUID> encantamientosGratis = ConcurrentHashMap.newKeySet();

    public EnchantingMinigameManager(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alAbrirMesa(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            Player p = (Player) event.getPlayer();

            // 15% de probabilidad de que aparezca el puzzle rúnico
            if (!runasActivas.containsKey(p.getUniqueId()) && Math.random() <= 0.15) {
                invocarRunas(p);
            }
        }
    }

    private void invocarRunas(Player p) {
        Location centro = p.getLocation().add(0, 1.5, 0); // Altura de los ojos

        List<ArmorStand> runas = new ArrayList<>();

        // 🌟 Componentes Ciberpunk nativos para Paper 1.21
        net.kyori.adventure.text.Component[] simbolos = {
                NexoColor.parse("&#5555FF<bold>🔵</bold>"), // Azul Neón
                NexoColor.parse("&#FF5555<bold>🔴</bold>"), // Rojo Alerta
                NexoColor.parse("&#55FF55<bold>🟢</bold>")  // Verde Éxito
        };

        for (int i = 0; i < 3; i++) {
            ArmorStand runa = p.getWorld().spawn(centro, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setGravity(false);
                as.setSmall(true);
                as.customName(simbolos[runas.size()]);
                as.setCustomNameVisible(true);
                as.setMarker(true); // Evita que se atoren
            });
            runas.add(runa);
        }

        runasActivas.put(p.getUniqueId(), runas);

        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2f);

        // Formato seguro para Titles
        p.sendTitle(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#AA00AA<bold>¡PUZZLE DE ENLACE!</bold>")),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#AAAAAAGolpea: &#5555FFAzul &#AAAAAA> &#FF5555Rojo &#AAAAAA> &#55FF55Verde")),
                5, 60, 5
        );

        // Animación orbital
        new BukkitRunnable() {
            double angulo = 0;
            int tiempo = 100; // 5 segundos

            @Override
            public void run() {
                if (tiempo <= 0 || !runasActivas.containsKey(p.getUniqueId()) || !p.isOnline()) {
                    limpiarRunas(p.getUniqueId());
                    cancel();
                    return;
                }

                // Rotar las runas alrededor del jugador
                for (int i = 0; i < runas.size(); i++) {
                    ArmorStand runa = runas.get(i);
                    if (runa.isDead()) continue;

                    double offset = angulo + (i * (Math.PI * 2 / 3));
                    double x = Math.cos(offset) * 1.5;
                    double z = Math.sin(offset) * 1.5;

                    runa.teleport(p.getLocation().add(x, 1.5, z));
                }

                angulo += 0.1;
                tiempo--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void alGolpearRuna(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand runa && event.getDamager() instanceof Player p) {
            if (!runasActivas.containsKey(p.getUniqueId())) return;

            List<ArmorStand> runas = runasActivas.get(p.getUniqueId());
            if (!runas.contains(runa)) return;

            event.setCancelled(true); // Evitar daño real al ArmorStand

            // Verificar si golpeó la que tocaba (la primera de la lista)
            if (runas.get(0).equals(runa)) {
                // Runa correcta
                p.playSound(runa.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
                runa.getWorld().spawnParticle(Particle.ENCHANT, runa.getLocation().add(0, 0.5, 0), 10);
                runa.remove();
                runas.remove(0);

                if (runas.isEmpty()) {
                    // ¡Completó el puzzle!
                    encantamientosGratis.add(p.getUniqueId());
                    runasActivas.remove(p.getUniqueId());
                    p.sendMessage(NexoColor.parse("&#AA00AA✨ <bold>SISTEMA HACKEADO:</bold> &#AAAAAATu próximo ensamblaje será 100% &#55FF55<bold>GRATUITO</bold>&#AAAAAA."));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                }
            } else {
                // Runa incorrecta (Fallo)
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                p.sendMessage(NexoColor.parse("&#FF5555[!] Secuencia biométrica incorrecta. La energía se ha dispersado."));
                limpiarRunas(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void alEncantar(EnchantItemEvent event) {
        Player p = event.getEnchanter();
        if (encantamientosGratis.contains(p.getUniqueId())) {
            // Aplicar descuento total
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.setLevel(p.getLevel() + event.getExpLevelCost());
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }, 1L); // Devolvemos los niveles un tick después

            encantamientosGratis.remove(p.getUniqueId());
        }
    }

    private void limpiarRunas(UUID id) {
        if (runasActivas.containsKey(id)) {
            for (ArmorStand as : runasActivas.get(id)) {
                if (as != null && !as.isDead()) {
                    as.getWorld().spawnParticle(Particle.SMOKE, as.getLocation(), 5);
                    as.remove();
                }
            }
            runasActivas.remove(id);
        }
    }
}