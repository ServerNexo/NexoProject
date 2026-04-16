package me.nexo.dungeons.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Gestor de Combates contra Jefes Globales (Arquitectura Enterprise)
 */
@Singleton
public class BossFightManager implements Listener {

    private final NexoDungeons plugin;
    private final LootDistributor lootDistributor;

    // Mapa tridimensional Thread-Safe: UUID del Boss -> (UUID del Jugador -> Daño Acumulado)
    private final Map<UUID, Map<UUID, Double>> activeBosses = new ConcurrentHashMap<>();

    // Lista de nombres internos de MythicMobs que son considerados "Bosses Públicos"
    private final Set<String> trackedBossTypes = Set.of("NexoDragon", "ReyEsqueleto", "TitanDeMagma");

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public BossFightManager(NexoDungeons plugin, LootDistributor lootDistributor) {
        this.plugin = plugin;
        this.lootDistributor = lootDistributor; // Inyectamos el repartidor de botín
    }

    // 🟢 1. Detectar cuando un Boss nace
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicSpawn(MythicMobSpawnEvent event) {
        if (trackedBossTypes.contains(event.getMobType().getInternalName())) {
            activeBosses.put(event.getEntity().getUniqueId(), new ConcurrentHashMap<>());
            plugin.getLogger().info("🐉 [BOSS FIGHT] Iniciando rastreo de daño para el Titán: " + event.getMobType().getInternalName());
        }
    }

    // ⚔️ 2. Rastrear cada golpe de forma ultra-rápida
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Verificación O(1): Si no es un boss rastreado, ignoramos al instante para no dar lag
        if (!activeBosses.containsKey(entityId)) return;

        Player atacante = null;
        if (event.getDamager() instanceof Player p) {
            atacante = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            atacante = p; // Daño válido con arcos o magia
        }

        if (atacante != null) {
            // Sumamos el daño final al historial del jugador de forma atómica y segura
            activeBosses.get(entityId).merge(atacante.getUniqueId(), event.getFinalDamage(), Double::sum);
        }
    }

    // 💀 3. El Boss muere: Hora de calcular el botín
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicDeath(MythicMobDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Removemos el boss de la memoria y extraemos su historial de daño
        Map<UUID, Double> damageMap = activeBosses.remove(entityId);

        if (damageMap != null && !damageMap.isEmpty()) {

            // 🌟 FIX SEGURIDAD ASÍNCRONA: Extraemos datos nativos antes de saltar al hilo asíncrono
            final String bossName = event.getMobType().getInternalName();
            final Location deathLoc = event.getEntity().getLocation().clone(); // ¡Clonado vital para evitar Crash!

            // 🚀 Java 21 Virtual Threads: Cálculo asíncrono ultrarrápido para no congelar el servidor
            Thread.startVirtualThread(() -> {

                // 🌟 FIX: Adiós al anti-patrón static. Usamos el LootDistributor inyectado
                // Le pasamos la 'deathLoc' para que sepa exactamente dónde soltar las recompensas
                lootDistributor.distributeLoot(bossName, damageMap, deathLoc);

            });
        }
    }
}