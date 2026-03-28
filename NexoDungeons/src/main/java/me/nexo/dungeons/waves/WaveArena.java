package me.nexo.dungeons.waves;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nexo.core.utils.NexoColor;
import me.nexo.dungeons.NexoDungeons;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WaveArena {

    private final String arenaId;
    private final Location spawnCenter;
    private int currentWave;
    private final Set<UUID> activeMythicMobs;
    private boolean isActive;

    public WaveArena(String arenaId, Location spawnCenter) {
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

        if (this.currentWave > 1 && (this.currentWave - 1) % 5 == 0) {
            int checkpoint = this.currentWave - 1;
            spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
                if (e instanceof org.bukkit.entity.Player p) {
                    p.sendMessage(NexoColor.parse("&#00f5ff<bold>¡PUNTO DE CONTROL!</bold> &#1c0f2aSimulación asegurada en la Oleada &#ff00ff" + checkpoint));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            });
        }

        spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
            if (e instanceof org.bukkit.entity.Player p) {
                Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500));
                Title title = Title.title(
                        NexoColor.parse("&#8b0000<bold>SIMULACIÓN " + currentWave + "</bold>"),
                        NexoColor.parse("&#ff00ff¡Prepárate para el impacto!"),
                        times
                );
                p.showTitle(title);
                p.playSound(p.getLocation(), org.bukkit.Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
            }
        });

        Bukkit.getScheduler().runTaskLater(NexoDungeons.getPlugin(NexoDungeons.class), this::spawnMobs, 60L);
    }

    private void spawnMobs() {
        int mobsToSpawn = 3 + (currentWave * 2);
        String mythicMobType = currentWave % 5 == 0 ? "NexoBossMinion" : "NexoGuerrero";

        for (int i = 0; i < mobsToSpawn; i++) {
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            Location spawnLoc = spawnCenter.clone().add(offsetX, 0, offsetZ);

            Entity spawnedEntity = MythicBukkit.inst().getMobManager().spawnMob(mythicMobType, spawnLoc).getEntity().getBukkitEntity();

            if (spawnedEntity instanceof LivingEntity livingMob) {
                this.activeMythicMobs.add(livingMob.getUniqueId());
                escalarAtributos(livingMob);
            }
        }
    }

    private void escalarAtributos(LivingEntity mob) {
        double multiplier = Math.pow(1.2, Math.max(0, currentWave - 1));

        AttributeInstance healthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * multiplier;
            healthAttr.setBaseValue(newHealth);
            mob.setHealth(newHealth);
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
                Bukkit.getScheduler().runTaskLater(NexoDungeons.getPlugin(NexoDungeons.class), this::nextWave, 40L);
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