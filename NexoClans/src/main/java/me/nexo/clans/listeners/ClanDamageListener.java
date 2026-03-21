package me.nexo.clans.listeners;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ClanDamageListener implements Listener {

    private final NexoClans plugin;
    private final NexoCore core;

    public ClanDamageListener(NexoClans plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        // 🌟 CORRECCIÓN: Usamos una variable temporal primero
        Player tempAttacker = null;
        if (event.getDamager() instanceof Player p) {
            tempAttacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            tempAttacker = p;
        }

        if (tempAttacker == null || tempAttacker.equals(victim)) return;

        // 🌟 La "sellamos" como final para que Java nos deje usarla en el Lambda
        final Player attacker = tempAttacker;

        NexoUser userAttacker = core.getUserManager().getUserOrNull(attacker.getUniqueId());
        NexoUser userVictim = core.getUserManager().getUserOrNull(victim.getUniqueId());

        if (userAttacker != null && userVictim != null &&
                userAttacker.hasClan() && userVictim.hasClan() &&
                userAttacker.getClanId().equals(userVictim.getClanId())) {

            plugin.getClanManager().getClanFromCache(userAttacker.getClanId()).ifPresent(clan -> {
                if (!clan.isFriendlyFire()) {
                    // 🛡️ ¡Escudo activado!
                    event.setCancelled(true);
                    attacker.sendMessage("§c¡No puedes herir a los aliados de tu clan! (Fuego Amigo Desactivado)");
                }
            });
        }
    }
}