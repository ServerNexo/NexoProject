package me.nexo.dungeons.waves;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 🏰 NexoDungeons - Instancia de Arena de Oleadas (Arquitectura Enterprise)
 * Nota: Instanciada dinámicamente por el WaveManager. No requiere Inyección.
 */
public class WaveArena {

    private final NexoDungeons plugin;
    private final String arenaId;
    private final Location spawnCenter;
    private int currentWave;
    private final Set<UUID> activeMythicMobs;
    private boolean isActive;

    public WaveArena(NexoDungeons plugin, String arenaId, Location spawnCenter) {
        this.plugin = plugin;
        this.arenaId = arenaId;
        this.spawnCenter = spawnCenter;
        this.currentWave = 0;
        this.activeMythicMobs = new HashSet<>();
        this.isActive = false;
    }

    public void start() {
        this.isActive = true;
        this.currentWave = 0;
        nextWave();
    }

    public void nextWave() {
        if (!isActive) return;
        this.currentWave++;

        // Punto de Control cada 5 oleadas
        if (this.currentWave > 1 && (this.currentWave - 1) % 5 == 0) {
            int checkpoint = this.currentWave - 1;
            spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
                if (e instanceof Player p) {
                    // 🌟 FIX: Mensaje directo, 0 lag de I/O
                    CrossplayUtils.sendMessage(p, "&#FFAA00[!] <bold>PUNTO DE CONTROL:</bold> &#E6CCFFHas sobrevivido hasta la oleada &#55FF55" + checkpoint + "&#E6CCFF.");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            });
        }

        // Anuncio de nueva oleada
        spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
            if (e instanceof Player p) {
                // 🌟 FIX: Títulos limpios y seguros para Bedrock
                CrossplayUtils.sendTitle(p,
                        "&#FF5555<bold>OLEADA " + currentWave + "</bold>",
                        "&#E6CCFFDefiende la zona..."
                );
                p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
            }
        });

        // Retraso de 3 segundos para que los jugadores se preparen
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnMobs, 60L);
    }

    private void spawnMobs() {
        int mobsToSpawn = 3 + (currentWave * 2);
        String mythicMobType = currentWave % 5 == 0 ? "NexoBossMinion" : "NexoGuerrero";

        // 🌟 FIX: Verificación segura del Mob de MythicMobs antes de intentar spawnearlo
        MythicMob mobType = MythicBukkit.inst().getMobManager().getMythicMob(mythicMobType).orElse(null);
        if (mobType == null) {
            plugin.getLogger().warning("⚠️ CRÍTICO: No se encontró el MythicMob '" + mythicMobType + "'. La oleada se ha estancado.");
            return;
        }

        for (int i = 0; i < mobsToSpawn; i++) {
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            Location spawnLoc = spawnCenter.clone().add(offsetX, 0, offsetZ);

            Entity spawnedEntity = MythicBukkit.inst().getMobManager().spawnMob(mythicMobType, spawnLoc).getEntity().getBukkitEntity();

            if (spawnedEntity instanceof LivingEntity livingMob) {
                this.activeMythicMobs.add(livingMob.getUniqueId());
                escalarAtributos(livingMob); // 💪 Hacemos a los mobs más fuertes
            }
        }
    }

    private void escalarAtributos(LivingEntity mob) {
        // Incrementa las stats un 20% por cada oleada
        double multiplier = Math.pow(1.2, Math.max(0, currentWave - 1));

        AttributeInstance healthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * multiplier;
            healthAttr.setBaseValue(newHealth);
            mob.setHealth(newHealth); // Lo curamos a su nueva vida máxima
        }

        AttributeInstance damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            double newDamage = damageAttr.getBaseValue() * multiplier;
            damageAttr.setBaseValue(newDamage);
        }
    }

    public void registrarMuerteMob(UUID mobId) {
        if (!isActive) return;

        if (activeMythicMobs.remove(mobId)) {
            if (activeMythicMobs.isEmpty()) {
                // 🌟 FIX: Siguiente oleada con delay de 2 segundos para que los jugadores respiren
                Bukkit.getScheduler().runTaskLater(plugin, this::nextWave, 40L);
            }
        }
    }

    public void stop() {
        this.isActive = false;
        this.activeMythicMobs.clear();
    }

    public String getArenaId() { return arenaId; }
    public boolean isActive() { return isActive; }
    public int getCurrentWave() { return currentWave; }
}