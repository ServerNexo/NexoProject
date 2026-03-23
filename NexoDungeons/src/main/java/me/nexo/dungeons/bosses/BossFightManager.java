package me.nexo.dungeons.bosses;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import me.nexo.dungeons.NexoDungeons;
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

public class BossFightManager implements Listener {

    private final NexoDungeons plugin;

    // Mapa tridimensional: UUID del Boss -> (UUID del Jugador -> Daño Acumulado)
    private final Map<UUID, Map<UUID, Double>> activeBosses = new ConcurrentHashMap<>();

    // Lista de nombres internos de MythicMobs que son considerados "Bosses Públicos"
    private final Set<String> trackedBossTypes = Set.of("NexoDragon", "ReyEsqueleto", "TitanDeMagma");

    public BossFightManager(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // 🟢 1. Detectar cuando un Boss nace
    @EventHandler
    public void onMythicSpawn(MythicMobSpawnEvent event) {
        if (trackedBossTypes.contains(event.getMobType().getInternalName())) {
            activeBosses.put(event.getEntity().getUniqueId(), new ConcurrentHashMap<>());
            plugin.getLogger().info("🐉 Iniciando rastreo de daño para el Boss Público: " + event.getMobType().getInternalName());
        }
    }

    // ⚔️ 2. Rastrear cada golpe de forma precisa
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Si no es un boss rastreado, ignoramos (para no dar lag)
        if (!activeBosses.containsKey(entityId)) return;

        Player atacante = null;
        if (event.getDamager() instanceof Player p) {
            atacante = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            atacante = p; // Daño con arcos o magia
        }

        if (atacante != null) {
            // Sumamos el daño final al historial del jugador
            activeBosses.get(entityId).merge(atacante.getUniqueId(), event.getFinalDamage(), Double::sum);
        }
    }

    // 💀 3. El Boss muere: Hora de calcular el botín
    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Removemos el boss de la memoria y extraemos su historial de daño
        Map<UUID, Double> damageMap = activeBosses.remove(entityId);

        if (damageMap != null && !damageMap.isEmpty()) {
            // 🚀 Mandamos a calcular el botín asíncronamente para no congelar el servidor
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                LootDistributor.distributeLoot(event.getMobType().getInternalName(), damageMap);
            });
        }
    }
}