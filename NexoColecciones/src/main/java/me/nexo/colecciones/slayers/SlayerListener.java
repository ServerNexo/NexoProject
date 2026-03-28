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

    private static final String BC_DIVIDER = "&#1c0f2a=======================================";
    private static final String MSG_COMPLETED = "&#00f5ff<bold>⚔️ ¡CONTRATO COMPLETADO!</bold>";
    private static final String MSG_BOSS_DEFEATED = "&#1c0f2aObjetivo eliminado: &#8b0000%boss%";
    private static final String MSG_REWARD = "&#00f5ff💎 ¡Transferencia de &#ff00ff%gems% Gemas &#00f5ffrealizada a tu cuenta!";
    private static final String MSG_BOSS_SPAWN_TITLE = "&#8b0000<bold>¡AMENAZA DETECTADA!</bold>";
    private static final String MSG_BOSS_SPAWN_DESC = "&#1c0f2aEl objetivo &#8b0000%boss% &#1c0f2aha entrado en tu área.";
    private static final String ERR_BOSS_TYPE = "&#8b0000[Error Interno] El tipo de entidad en la base de datos es inválido.";

    public SlayerListener(NexoColecciones plugin) {
        this.plugin = plugin;
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

                    CrossplayUtils.sendMessage(bossOwner, BC_DIVIDER);
                    CrossplayUtils.sendMessage(bossOwner, MSG_COMPLETED);
                    CrossplayUtils.sendMessage(bossOwner, MSG_BOSS_DEFEATED.replace("%boss%", slayer.getBossName()));
                    CrossplayUtils.sendMessage(bossOwner, BC_DIVIDER);

                    int recompensaGemas = slayer.getTemplate().requiredKills() / 10;
                    NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(bossOwner.getUniqueId());
                    if (user != null) {
                        user.addGems(recompensaGemas);
                        CrossplayUtils.sendMessage(bossOwner, MSG_REWARD.replace("%gems%", String.valueOf(recompensaGemas)));
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

                            CrossplayUtils.sendMessage(killer, MSG_BOSS_SPAWN_TITLE);
                            CrossplayUtils.sendMessage(killer, MSG_BOSS_SPAWN_DESC.replace("%boss%", slayer.getBossName()));

                        } catch (Exception e) {
                            CrossplayUtils.sendMessage(killer, ERR_BOSS_TYPE);
                        }
                    }
                }
            }
        }
    }
}