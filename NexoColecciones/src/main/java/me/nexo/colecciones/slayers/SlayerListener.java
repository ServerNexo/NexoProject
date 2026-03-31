package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class SlayerListener implements Listener {

    private final NexoColecciones plugin;
    private final NexoCore core;

    public SlayerListener(NexoColecciones plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    // 🌟 CORRECCIÓN: Leemos los mensajes directamente de la configuración local de NexoColecciones
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    // ==========================================
    // 🛡️ PARCHE ANTI-ROBOS DE JEFES
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlayerDamage(EntityDamageByEntityEvent event) {
        // Verificamos si la entidad atacada es un Jefe Slayer
        if (event.getEntity().hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = event.getEntity().getMetadata("SlayerBoss").get(0).asString();

            Player damager = null;
            if (event.getDamager() instanceof Player p) {
                damager = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                damager = p;
            }

            if (damager != null) {
                // Si el que ataca no es el dueño ni un Admin, bloqueamos el daño
                if (!bossOwnerUUID.equals(damager.getUniqueId().toString()) && !damager.hasPermission("nexoslayer.admin")) {
                    event.setCancelled(true);
                    // 🌟 COLOR APLICADO: Lila iluminado para mejor lectura
                    CrossplayUtils.sendMessage(damager, "&#FF3366[!] Herejía: &#E6CCFFEste monstruo fue invocado por otro mortal. Tus golpes no surten efecto.");
                }
            } else {
                // Evitamos que fuego, lava u otros monstruos (ej. golems) maten al jefe y arruinen la cacería
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // 🗡️ GESTIÓN DE MUERTES (Invocación y Recompensas)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 1. SI MUERE EL JEFE SLAYER
        if (entity.hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = entity.getMetadata("SlayerBoss").get(0).asString();
            Player bossOwner = Bukkit.getPlayer(UUID.fromString(bossOwnerUUID));

            if (bossOwner != null) {
                ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(bossOwner.getUniqueId());

                if (slayer != null && slayer.isBossSpawned()) {
                    if (slayer.getBossBar() != null) {
                        slayer.getBossBar().removeAll();
                    }

                    CrossplayUtils.sendMessage(bossOwner, getMessage("eventos.jefe-slayer.contrato-completado"));
                    CrossplayUtils.sendMessage(bossOwner, getMessage("eventos.jefe-slayer.jefe-derrotado").replace("%boss%", slayer.getTemplate().bossName()));

                    // Recompensa de Gemas
                    int recompensaGemas = slayer.getTemplate().requiredKills() / 10;
                    NexoUser user = core.getUserManager().getUserOrNull(bossOwner.getUniqueId());
                    if (user != null) {
                        user.addGems(recompensaGemas);
                        CrossplayUtils.sendMessage(bossOwner, getMessage("eventos.jefe-slayer.recompensa-gemas").replace("%gems%", String.valueOf(recompensaGemas)));
                    }

                    plugin.getCollectionManager().addProgress(bossOwner, slayer.getTemplate().id(), 1);
                    plugin.getSlayerManager().removeActiveSlayer(bossOwner.getUniqueId());
                }
            }
            return;
        }

        // 2. SI MUERE UN MOB NORMAL (Progreso del Contrato)
        if (killer != null) {
            ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(killer.getUniqueId());

            if (slayer != null && !slayer.isBossSpawned()) {
                if (entity.getType().name().equalsIgnoreCase(slayer.getTemplate().targetMob())) {
                    slayer.addKill();

                    // Si ya cumplió las kills requeridas, invocamos al Jefe
                    if (slayer.getKills() >= slayer.getTemplate().requiredKills()) {
                        slayer.setBossSpawned(true);
                        Location loc = entity.getLocation();

                        try {
                            EntityType type = EntityType.valueOf(slayer.getTemplate().bossType());
                            LivingEntity boss = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

                            boss.customName(CrossplayUtils.parseCrossplay(killer, "&#8b0000<bold>" + slayer.getTemplate().bossName() + "</bold>"));
                            boss.setCustomNameVisible(true);
                            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000.0);
                            boss.setHealth(1000.0);

                            // Ponemos la marca para que solo el dueño pueda pegarle
                            boss.setMetadata("SlayerBoss", new FixedMetadataValue(plugin, killer.getUniqueId().toString()));

                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.spawn-titulo"));
                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.spawn-descripcion").replace("%boss%", slayer.getTemplate().bossName()));

                        } catch (Exception e) {
                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.error-tipo-jefe"));
                        }
                    }
                }
            }
        }
    }
}