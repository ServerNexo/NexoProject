package me.nexo.mechanics.minigames;

import me.nexo.mechanics.NexoMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.core.ClaimAction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FarmingMinigameManager implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos el plugin de Mechanics
    private final NexoMechanics plugin;

    // UUID del ArmorStand (La Plaga) -> Cantidad de golpes recibidos
    private final Map<UUID, Integer> plagasActivas = new ConcurrentHashMap<>();

    public FarmingMinigameManager(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alCosechar(BlockBreakEvent event) {
        Player p = event.getPlayer();

        // 🌟 INTEGRACIÓN: Prevenir invocación de entidades en granjas enemigas
        if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(event.getBlock().getLocation());
            if (stone != null && !stone.hasPermission(p.getUniqueId(), me.nexo.protections.core.ClaimAction.BREAK)) {
                return; // Cortamos el flujo: No se invocan plagas en territorio ajeno
            }
        }

        if (event.getBlock().getBlockData() instanceof Ageable cultivo) {
            // Solo si está maduro y tiene 1% de probabilidad
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.01) {
                invocarPlagaMutante(p, event.getBlock().getLocation().add(0.5, 0, 0.5));
            }
        }
    }

    private void invocarPlagaMutante(Player p, org.bukkit.Location loc) {
        // Solución al error de IntelliJ: Declaración secuencial clásica
        ArmorStand plaga = p.getWorld().spawn(loc, ArmorStand.class);
        plaga.setInvisible(true);
        plaga.setSmall(true); // Tamaño más pequeño y molesto

        if (plaga.getEquipment() != null) {
            plaga.getEquipment().setHelmet(new ItemStack(Material.WEEPING_VINES)); // Textura de Raíz Corrupta
        }

        plaga.setCustomName("§c§lPlaga Mutante");
        plaga.setCustomNameVisible(true);

        plagasActivas.put(plaga.getUniqueId(), 0);
        p.playSound(loc, Sound.ENTITY_SILVERFISH_AMBIENT, 1f, 0.5f);
        p.sendTitle("§2§l¡PLAGA MUTANTE!", "§aDestrúyela antes de que escape", 5, 40, 5);

        // BukkitRunnable para que "salte" como un bicho por los cultivos
        new BukkitRunnable() {
            int tiempoVida = 20; // 10 segundos (20 ticks x medio segundo)

            @Override
            public void run() {
                if (tiempoVida <= 0 || plaga.isDead() || !plagasActivas.containsKey(plaga.getUniqueId())) {
                    if (!plaga.isDead()) {
                        plaga.getWorld().spawnParticle(Particle.SMOKE, plaga.getLocation(), 10); // 🟢 FIX 1.21: Particle Name
                        plaga.remove();
                        plagasActivas.remove(plaga.getUniqueId());
                    }
                    cancel();
                    return;
                }

                // Salto aleatorio en direcciones X o Z
                Vector salto = new Vector((Math.random() - 0.5) * 1.5, 0.6, (Math.random() - 0.5) * 1.5);
                plaga.setVelocity(salto);
                plaga.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, plaga.getLocation(), 5);

                tiempoVida--;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Salta cada medio segundo
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alGolpearPlaga(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand plaga && plagasActivas.containsKey(plaga.getUniqueId())) {
            event.setCancelled(true); // Evitamos que el jugador rompa el ArmorStand a la primera

            if (event.getDamager() instanceof Player p) {
                int golpes = plagasActivas.get(plaga.getUniqueId()) + 1;

                // Efecto de daño
                p.playSound(plaga.getLocation(), Sound.ENTITY_SLIME_HURT, 1f, 1f + (golpes * 0.2f));
                plaga.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, plaga.getLocation().add(0, 1, 0), 3);

                if (golpes >= 5) {
                    // ¡Destruida!
                    plagasActivas.remove(plaga.getUniqueId());
                    plaga.remove();

                    p.playSound(plaga.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
                    plaga.getWorld().spawnParticle(Particle.EXPLOSION, plaga.getLocation(), 1);

                    // Drops raros
                    plaga.getWorld().dropItemNaturally(plaga.getLocation(), new ItemStack(Material.PITCHER_POD, 3));
                    p.sendActionBar("§a¡La plaga fue erradicada!");
                } else {
                    plagasActivas.put(plaga.getUniqueId(), golpes);
                }
            }
        }
    }
}