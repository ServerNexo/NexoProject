package me.nexo.pvp.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PvPListener implements Listener {

    private final PvPManager manager;

    public PvPListener(PvPManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDañoJugadores(EntityDamageByEntityEvent event) {
        Player atacante = null;
        if (event.getDamager() instanceof Player p) atacante = p;
        else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) atacante = p;

        if (atacante != null && event.getEntity() instanceof Player victima) {
            if (atacante.equals(victima)) return;

            if (!manager.tienePvP(atacante) || !manager.tienePvP(victima)) {
                event.setCancelled(true);
                return;
            }

            manager.marcarEnCombate(atacante, victima);
            // Reducción de daño RPG al 40% (60% menos de daño)
            event.setDamage(event.getDamage() * 0.40);
        }
    }

    // ==========================================
    // 🏆 SISTEMA DE HONOR Y BOUNTY
    // ==========================================
    @EventHandler
    public void onMuerte(PlayerDeathEvent event) {
        Player victima = event.getEntity();
        Player asesino = victima.getKiller();
        UUID idVictima = victima.getUniqueId();

        // 1. Limpiamos combate de la víctima
        manager.enCombate.remove(idVictima);

        if (asesino != null && manager.tienePvP(victima) && manager.tienePvP(asesino)) {
            UUID idAsesino = asesino.getUniqueId();

            // 2. Dar Honor Base (+1)
            int honorActual = manager.puntosHonor.getOrDefault(idAsesino, 0) + 1;
            manager.puntosHonor.put(idAsesino, honorActual);
            asesino.sendMessage("§e⚔ ¡Has derrotado a " + victima.getName() + "! §6(+1 Honor)");

            // 3. Sistema de Bounty (Cazarrecompensas)
            int rachaVictima = manager.rachaAsesinatos.getOrDefault(idVictima, 0);

            // Si la víctima tenía un Bounty (Racha >= 3)
            if (rachaVictima >= 3) {
                Bukkit.broadcastMessage("§e§l[CAZARRECOMPENSAS] §f" + asesino.getName() + " §aha cobrado la recompensa por la cabeza de §c" + victima.getName() + "§a!");
                asesino.sendMessage("§6💎 ¡Bounty Reclamado! Ganaste +5 de Honor extra y un Diamante.");
                manager.puntosHonor.put(idAsesino, honorActual + 5);
                asesino.getInventory().addItem(new ItemStack(Material.DIAMOND));
                asesino.playSound(asesino.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            // 4. Aumentar Racha del Asesino
            int rachaAsesino = manager.rachaAsesinatos.getOrDefault(idAsesino, 0) + 1;
            manager.rachaAsesinatos.put(idAsesino, rachaAsesino);

            // Anunciar si el asesino obtiene un Bounty
            if (rachaAsesino == 3) {
                Bukkit.broadcastMessage("§4§l[SE BUSCA] §c" + asesino.getName() + " §7está en una racha de sangre (3 Kills). ¡Hay recompensa por su cabeza!");
            } else if (rachaAsesino > 3) {
                Bukkit.broadcastMessage("§4§l[IMPARABLE] §c" + asesino.getName() + " §7ha llegado a " + rachaAsesino + " Kills!");
            }
        }

        // 5. Reiniciar Racha de la Víctima a 0
        manager.rachaAsesinatos.put(idVictima, 0);
    }

    @EventHandler
    public void onDesconexionCobarde(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (manager.estaEnCombate(p)) {
            p.setHealth(0.0);
            manager.enCombate.remove(p.getUniqueId());
            Bukkit.broadcastMessage("§c☠ §l" + p.getName() + " §cse desconectó cobardemente en combate.");
        }
    }
}