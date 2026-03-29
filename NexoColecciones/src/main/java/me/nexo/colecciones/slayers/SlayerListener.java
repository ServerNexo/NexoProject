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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class SlayerListener implements Listener {

    private final NexoColecciones plugin;
    private final NexoCore core;

    public SlayerListener(NexoColecciones plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("colecciones_messages.yml", path);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (entity.hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = entity.getMetadata("SlayerBoss").get(0).asString();
            Player bossOwner = Bukkit.getPlayer(UUID.fromString(bossOwnerUUID));

            if (bossOwner != null) {
                ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(bossOwner.getUniqueId());

                if (slayer != null && slayer.isBossSpawned()) {
                    slayer.getBossBar().removeAll();

                    CrossplayUtils.sendMessage(bossOwner, getMessage("eventos.jefe-slayer.contrato-completado"));
                    CrossplayUtils.sendMessage(bossOwner, getMessage("eventos.jefe-slayer.jefe-derrotado").replace("%boss%", slayer.getBossName()));

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

        if (killer != null) {
            ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(killer.getUniqueId());

            if (slayer != null && !slayer.isBossSpawned()) {
                if (entity.getType().name().equalsIgnoreCase(slayer.getTemplate().targetMob())) {
                    slayer.addKill();
                    if (slayer.getKills() >= slayer.getTemplate().requiredKills()) {
                        slayer.setBossSpawned(true);
                        Location loc = entity.getLocation();
                        try {
                            EntityType type = EntityType.valueOf(slayer.getTemplate().bossType());
                            LivingEntity boss = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

                            boss.customName(CrossplayUtils.parseCrossplay(killer, "&#8b0000<bold>" + slayer.getBossName() + "</bold>"));
                            boss.setCustomNameVisible(true);
                            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000.0);
                            boss.setHealth(1000.0);
                            boss.setMetadata("SlayerBoss", new org.bukkit.metadata.FixedMetadataValue(plugin, killer.getUniqueId().toString()));

                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.spawn-titulo"));
                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.spawn-descripcion").replace("%boss%", slayer.getBossName()));

                        } catch (Exception e) {
                            CrossplayUtils.sendMessage(killer, getMessage("eventos.jefe-slayer.error-tipo-jefe"));
                        }
                    }
                }
            }
        }
    }
}