package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SlayerListener implements Listener {

    private final NexoColecciones plugin;
    private final SlayerManager manager;

    public SlayerListener(NexoColecciones plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSlayerManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;

        // Revisamos si el jugador tiene una cacería activa
        ActiveSlayer slayer = manager.getActiveSlayer(player.getUniqueId());
        if (slayer == null) return;

        LivingEntity killed = event.getEntity();
        String mobName = killed.getType().name();

        // ==========================================
        // 1. ¿Acaba de matar al Boss final?
        // ==========================================
        if (slayer.isBossSpawned() && killed.hasMetadata("SlayerBoss_" + player.getUniqueId().toString())) {

            player.sendMessage("§8=======================================");
            player.sendMessage("§a§l⚔️ ¡CACERÍA COMPLETADA!");
            player.sendMessage("§7Has derrotado a: §c" + slayer.getBossName());
            player.sendMessage("§8=======================================");

            // Disparamos las recompensas desde la consola
            for (String cmd : slayer.getRewards()) {
                String finalCmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Damos por terminada la misión
            manager.removeActiveSlayer(player.getUniqueId());
            return;
        }

        // ==========================================
        // 2. ¿Mató a un mob normal para llenar la barra?
        // ==========================================
        if (!slayer.isBossSpawned() && mobName.equals(slayer.getTargetMob())) {
            slayer.addKill();

            int current = slayer.getCurrentKills();
            int req = slayer.getRequiredKills();

            // Si ya llenó la barra, ¡INVOCAMOS AL BOSS!
            if (slayer.isReadyForBoss()) {
                player.sendTitle("§c§l¡CUIDADO!", "§7El " + slayer.getBossName() + " §7ha aparecido", 10, 70, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

                Location spawnLoc = killed.getLocation();
                spawnLoc.getWorld().strikeLightningEffect(spawnLoc); // Efecto visual épico de rayo

                try {
                    EntityType bossType = EntityType.valueOf(slayer.getBossType());
                    Entity boss = spawnLoc.getWorld().spawnEntity(spawnLoc, bossType);
                    boss.setCustomName(slayer.getBossName());
                    boss.setCustomNameVisible(true);

                    // 🌟 PROTECCIÓN: Le ponemos el UUID del jugador al Boss para que nadie más pueda robárselo
                    boss.setMetadata("SlayerBoss_" + player.getUniqueId().toString(), new FixedMetadataValue(plugin, true));

                    slayer.setBossSpawned(true);

                } catch (IllegalArgumentException e) {
                    player.sendMessage("§c[Error Interno] El tipo de Boss en el YAML es inválido.");
                    manager.removeActiveSlayer(player.getUniqueId());
                }
            } else {
                // Si aún no llena la barra, le mostramos el progreso en la Action Bar
                player.sendActionBar("§c⚔️ Progreso de Slayer: §e" + current + " §8/ §e" + req);
            }
        }
    }
}