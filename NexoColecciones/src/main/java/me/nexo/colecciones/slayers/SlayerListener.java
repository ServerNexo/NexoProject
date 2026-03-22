package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.economy.NexoEconomy; // 🌟 IMPORT ECONOMÍA
import me.nexo.economy.core.NexoAccount; // 🌟 IMPORT DIVISAS
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.math.BigDecimal; // 🌟 IMPORT PARA LOS NÚMEROS EXACTOS

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

        ActiveSlayer slayer = manager.getActiveSlayer(player.getUniqueId());
        if (slayer == null) return;

        LivingEntity killed = event.getEntity();
        String mobName = killed.getType().name();

        // 1. ¿Mató al Boss Final?
        if (slayer.isBossSpawned() && killed.hasMetadata("SlayerBoss_" + player.getUniqueId().toString())) {

            player.sendMessage("§8=======================================");
            player.sendMessage("§a§l⚔️ ¡CACERÍA COMPLETADA!");
            player.sendMessage("§7Has derrotado a: §c" + slayer.getBossName());
            player.sendMessage("§8=======================================");

            // Ejecutamos los comandos de recompensa normales del YAML
            for (String cmd : slayer.getRewards()) {
                String finalCmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // 🌟 NUEVO: Borramos la BossBar al ganar
            if (slayer.getBossBar() != null) {
                slayer.getBossBar().removeAll();
            }

            // 💎 INYECCIÓN DE ECONOMÍA: Recompensar con Gemas
            BigDecimal recompensaGemas = new BigDecimal("5"); // 5 Gemas por Boss
            NexoEconomy.getPlugin(NexoEconomy.class).getEconomyManager()
                    .updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.GEMS, recompensaGemas, true)
                    .thenAccept(success -> {
                        if (success) {
                            player.sendMessage("§a💎 ¡Has obtenido " + recompensaGemas + " Gemas por derrotar al Slayer!");
                        }
                    });

            manager.removeActiveSlayer(player.getUniqueId());
            return;
        }

        // 2. ¿Mató a un mob normal?
        if (!slayer.isBossSpawned() && mobName.equals(slayer.getTargetMob())) {
            slayer.addKill();

            int current = slayer.getCurrentKills();
            int req = slayer.getRequiredKills();

            if (slayer.isReadyForBoss()) {
                player.sendTitle("§c§l¡CUIDADO!", "§7El " + slayer.getBossName() + " §7ha aparecido", 10, 70, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

                Location spawnLoc = killed.getLocation();
                spawnLoc.getWorld().strikeLightningEffect(spawnLoc);

                try {
                    LivingEntity boss = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.valueOf(slayer.getBossType()));
                    boss.setCustomName(slayer.getBossName());
                    boss.setCustomNameVisible(true);
                    boss.setMetadata("SlayerBoss_" + player.getUniqueId().toString(), new FixedMetadataValue(plugin, true));

                    slayer.setBossSpawned(true);

                    // Creamos la BossBar y se la mostramos al jugador
                    BossBar bar = Bukkit.createBossBar(slayer.getBossName(), BarColor.RED, BarStyle.SOLID);
                    bar.addPlayer(player);
                    slayer.setBossBar(bar);

                } catch (IllegalArgumentException e) {
                    player.sendMessage("§c[Error] El tipo de Boss en el YAML es inválido.");
                    manager.removeActiveSlayer(player.getUniqueId());
                }
            } else {
                player.sendActionBar("§c⚔️ Progreso de Slayer: §e" + current + " §8/ §e" + req);
            }
        }
    }

    // Actualizar la barra cuando el Boss recibe daño
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity boss)) return;

        for (ActiveSlayer slayer : manager.getActiveSlayers().values()) {
            if (slayer.isBossSpawned() && boss.hasMetadata("SlayerBoss_" + slayer.getPlayerId().toString())) {
                if (slayer.getBossBar() != null) {
                    double maxHealth = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
                    double currentHealth = boss.getHealth() - event.getFinalDamage();
                    if (currentHealth < 0) currentHealth = 0;

                    double progress = currentHealth / maxHealth;
                    slayer.getBossBar().setProgress(progress);
                }
                break;
            }
        }
    }

    // Castigar al jugador si se desconecta (Se cancela la misión)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ActiveSlayer slayer = manager.getActiveSlayer(event.getPlayer().getUniqueId());
        if (slayer != null) {
            if (slayer.getBossBar() != null) {
                slayer.getBossBar().removeAll(); // Quitamos la barra
            }
            manager.removeActiveSlayer(event.getPlayer().getUniqueId());
        }
    }
}