package me.nexo.dungeons.waves;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WaveArena {

    private final String arenaId;
    private final Location spawnCenter;
    private int currentWave;
    private final Set<UUID> activeMythicMobs; // Guarda los UUIDs de los monstruos vivos
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

        // 🌟 SISTEMA DE CHECKPOINTS (Cada 5 oleadas)
        if (this.currentWave > 1 && (this.currentWave - 1) % 5 == 0) {
            int checkpoint = this.currentWave - 1;
            spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
                if (e instanceof org.bukkit.entity.Player p) {
                    p.sendMessage("§a§l¡CHECKPOINT ALCANZADO! §7Has asegurado la Oleada " + checkpoint);
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

                    // Aquí guardaríamos en Supabase o en NexoEconomy el progreso del jugador
                    // Ej: me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class).getUserManager().getUser(p.getUniqueId()).setMaxWave(checkpoint);
                }
            });
        }

        // Mensaje global en la zona de la arena
        spawnCenter.getWorld().getNearbyEntities(spawnCenter, 30, 30, 30).forEach(e -> {
            if (e instanceof org.bukkit.entity.Player p) {
                p.sendTitle("§c§lOLEADA " + currentWave, "§e¡Prepárate para luchar!", 10, 50, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
            }
        });

        // Retraso de 3 segundos antes de spawnear a los monstruos
        Bukkit.getScheduler().runTaskLater(NexoDungeons.getPlugin(NexoDungeons.class), this::spawnMobs, 60L);
    }

    private void spawnMobs() {
        // 🌟 Lógica de Dificultad: Más oleadas = Más mobs y diferentes tipos
        int mobsToSpawn = 3 + (currentWave * 2);
        String mythicMobType = currentWave % 5 == 0 ? "NexoBossMinion" : "NexoGuerrero"; // Boss cada 5 oleadas

        for (int i = 0; i < mobsToSpawn; i++) {
            // Dispersión aleatoria alrededor del centro (Radio de 5 bloques)
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            Location spawnLoc = spawnCenter.clone().add(offsetX, 0, offsetZ);

            // Spawnear usando MythicMobs API
            Entity spawnedEntity = MythicBukkit.inst().getMobManager().spawnMob(mythicMobType, spawnLoc).getEntity().getBukkitEntity();

            if (spawnedEntity instanceof LivingEntity livingMob) {
                this.activeMythicMobs.add(livingMob.getUniqueId());
                escalarAtributos(livingMob);
            }
        }
    }

    private void escalarAtributos(LivingEntity mob) {
        // 📈 ESCALADO MATEMÁTICO: Salud = Base * (1.2 ^ (Oleada - 1))
        double multiplier = Math.pow(1.2, Math.max(0, currentWave - 1));

        AttributeInstance healthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * multiplier;
            healthAttr.setBaseValue(newHealth);
            mob.setHealth(newHealth);
        }

        AttributeInstance damageAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double newDamage = damageAttr.getBaseValue() * multiplier;
            damageAttr.setBaseValue(newDamage);
        }
    }

    public void registrarMuerteMob(UUID mobId) {
        if (!isActive) return;

        if (activeMythicMobs.remove(mobId)) {
            if (activeMythicMobs.isEmpty()) {
                // ¡La oleada fue limpiada!
                Bukkit.getScheduler().runTaskLater(NexoDungeons.getPlugin(NexoDungeons.class), this::nextWave, 40L); // 2 segundos de respiro

                // 🪙 Aquí puedes dar Recompensas/Monedas a los jugadores cercanos por limpiar la oleada
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