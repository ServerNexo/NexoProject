package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
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

    // 🎨 PALETA HEX - CONSTANTES (Clean Code)
    private static final String BC_DIVIDER = "&#434343=======================================";
    private static final String MSG_COMPLETED = "&#a8ff78<bold>⚔️ ¡CONTRATO COMPLETADO!</bold>";
    private static final String MSG_BOSS_DEFEATED = "&#434343Objetivo eliminado: &#ff4b2b%boss%";
    private static final String MSG_REWARD = "&#a8ff78💎 ¡Transferencia de %gems% Gemas realizada a tu cuenta!";
    private static final String MSG_BOSS_SPAWN_TITLE = "&#ff4b2b<bold>¡AMENAZA DETECTADA!</bold>";
    private static final String MSG_BOSS_SPAWN_DESC = "&#434343El objetivo &#ff4b2b%boss% &#434343ha entrado en tu área.";
    private static final String ERR_BOSS_TYPE = "&#ff4b2b[Error Interno] El tipo de entidad en la base de datos es inválido.";

    public SlayerListener(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 1. COMPROBAR SI MATAMOS A UN BOSS DE SLAYER
        if (entity.hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = entity.getMetadata("SlayerBoss").get(0).asString();
            Player bossOwner = Bukkit.getPlayer(UUID.fromString(bossOwnerUUID));

            if (bossOwner != null) {
                ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(bossOwner.getUniqueId());

                if (slayer != null && slayer.isBossSpawned()) {
                    // Borramos la BossBar
                    slayer.getBossBar().removeAll();

                    bossOwner.sendMessage(NexoColor.parse(BC_DIVIDER));
                    bossOwner.sendMessage(NexoColor.parse(MSG_COMPLETED));
                    bossOwner.sendMessage(NexoColor.parse(MSG_BOSS_DEFEATED.replace("%boss%", slayer.getBossName())));
                    bossOwner.sendMessage(NexoColor.parse(BC_DIVIDER));

                    // RECOMPENSA: Damos Gemas (Economía Premium)
                    int recompensaGemas = slayer.getTemplate().requiredKills() / 10;
                    NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(bossOwner.getUniqueId());
                    if (user != null) {
                        user.addGems(recompensaGemas);
                        bossOwner.sendMessage(NexoColor.parse(MSG_REWARD.replace("%gems%", String.valueOf(recompensaGemas))));
                    }

                    // 🌟 PARCHE C: SINERGIA CON COLECCIONES (El Boss suma a tu Grimorio)
                    plugin.getCollectionManager().addProgress(bossOwner, slayer.getTemplate().id(), 1);

                    // Terminamos el slayer
                    plugin.getSlayerManager().removeActiveSlayer(bossOwner.getUniqueId());
                }
            }
            return; // Si era un boss, ya no contamos su kill para el farmeo normal
        }

        // 2. COMPROBAR SI EL JUGADOR ESTÁ FARMEANDO PARA INVOCAR AL BOSS
        if (killer != null) {
            ActiveSlayer slayer = plugin.getSlayerManager().getActiveSlayer(killer.getUniqueId());

            if (slayer != null && !slayer.isBossSpawned()) {
                // Si el mob que mató es el que le pide su contrato
                if (entity.getType().name().equalsIgnoreCase(slayer.getTemplate().targetMob())) {

                    slayer.addKill();

                    // Si ya llegó a la meta, ¡SPAWNEA EL BOSS!
                    if (slayer.getKills() >= slayer.getTemplate().requiredKills()) {
                        slayer.setBossSpawned(true);

                        Location loc = entity.getLocation();
                        try {
                            EntityType type = EntityType.valueOf(slayer.getTemplate().bossType());
                            LivingEntity boss = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

                            // Súper buff al Boss
                            boss.customName(NexoColor.parse("&#ff4b2b<bold>" + slayer.getBossName() + "</bold>"));
                            boss.setCustomNameVisible(true);
                            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000.0);
                            boss.setHealth(1000.0);

                            // Le ponemos la etiqueta para reconocerlo cuando muera
                            boss.setMetadata("SlayerBoss", new org.bukkit.metadata.FixedMetadataValue(plugin, killer.getUniqueId().toString()));

                            killer.sendMessage(NexoColor.parse(MSG_BOSS_SPAWN_TITLE));
                            killer.sendMessage(NexoColor.parse(MSG_BOSS_SPAWN_DESC.replace("%boss%", slayer.getBossName())));

                        } catch (Exception e) {
                            killer.sendMessage(NexoColor.parse(ERR_BOSS_TYPE));
                        }
                    }
                }
            }
        }
    }
}