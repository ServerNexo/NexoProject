package me.nexo.pvp.pvp;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
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

            // 🌟 VERIFICAR ZONA SEGURA (NexoProtections)
            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(victima.getLocation());

                if (stone != null && !stone.getFlag("pvp")) {

                    // 🌟 MODO HONOR: ¿Están en guerra? Si es así, IGNORAMOS la zona segura.
                    boolean ignorarProteccion = false;
                    if (Bukkit.getPluginManager().isPluginEnabled("NexoWar")) {
                        ignorarProteccion = me.nexo.war.NexoWar.getPlugin(me.nexo.war.NexoWar.class).getWarManager().estanEnGuerraActiva(atacante.getUniqueId(), victima.getUniqueId());
                    }

                    if (!ignorarProteccion) {
                        atacante.sendMessage(NexoColor.parse("&#FF5555[!] Bloqueo de Armamento: &#AAAAAAEl objetivo se encuentra en una zona neutral."));
                        event.setCancelled(true);
                        return; // Cortamos el evento antes de que entren en combate
                    } else {
                        // Opcional: Avisarle que está en territorio enemigo
                        // atacante.sendMessage(NexoColor.parse("&#FF5555[!] ALERTA TÁCTICA: Fuego autorizado en territorio hostil."));
                    }
                }
            }

            if (!manager.tienePvP(atacante) || !manager.tienePvP(victima)) {
                event.setCancelled(true);
                return;
            }

            manager.marcarEnCombate(atacante, victima);
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

        manager.enCombate.remove(idVictima);

        if (asesino != null && manager.tienePvP(victima) && manager.tienePvP(asesino)) {
            UUID idAsesino = asesino.getUniqueId();

            int honorActual = manager.puntosHonor.getOrDefault(idAsesino, 0) + 1;
            manager.puntosHonor.put(idAsesino, honorActual);
            asesino.sendMessage(NexoColor.parse("&#FFAA00⚔ <bold>OBJETIVO NEUTRALIZADO:</bold> &#AAAAAA" + victima.getName() + " &#55FF55(+1 Honor)"));

            int rachaVictima = manager.rachaAsesinatos.getOrDefault(idVictima, 0);

            if (rachaVictima >= 3) {
                // 🌟 CORRECCIÓN: Usamos Bukkit.broadcast() para enviar el Component
                Bukkit.broadcast(NexoColor.parse("&#00E5FF<bold>[CAZARRECOMPENSAS]</bold> &#FFFFFF" + asesino.getName() + " &#AAAAAAha cobrado el contrato por la cabeza de &#FF5555" + victima.getName() + "&#AAAAAA!"));

                asesino.sendMessage(NexoColor.parse("&#00E5FF[💎] <bold>Bounty Reclamado:</bold> &#AAAAAATransferencia de +5 Honor y recurso primario completada."));
                manager.puntosHonor.put(idAsesino, honorActual + 5);
                asesino.getInventory().addItem(new ItemStack(Material.DIAMOND));
                asesino.playSound(asesino.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            int rachaAsesino = manager.rachaAsesinatos.getOrDefault(idAsesino, 0) + 1;
            manager.rachaAsesinatos.put(idAsesino, rachaAsesino);

            if (rachaAsesino == 3) {
                // 🌟 CORRECCIÓN: Usamos Bukkit.broadcast()
                Bukkit.broadcast(NexoColor.parse("&#FF5555<bold>[OBJETIVO PRIORITARIO]</bold> &#FFFFFF" + asesino.getName() + " &#AAAAAAestá en racha letal (3 Kills). ¡Contrato de caza emitido!"));
            } else if (rachaAsesino > 3) {
                // 🌟 CORRECCIÓN: Usamos Bukkit.broadcast()
                Bukkit.broadcast(NexoColor.parse("&#FF5555<bold>[AMENAZA NIVEL OMEGA]</bold> &#FFFFFF" + asesino.getName() + " &#AAAAAAha alcanzado " + rachaAsesino + " Kills consecutivas!"));
            }
        }

        manager.rachaAsesinatos.put(idVictima, 0);
    }

    @EventHandler
    public void onDesconexionCobarde(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (manager.estaEnCombate(p)) {
            p.setHealth(0.0);
            manager.enCombate.remove(p.getUniqueId());
            // 🌟 CORRECCIÓN: Usamos Bukkit.broadcast() en lugar de Bukkit.broadcastMessage()
            Bukkit.broadcast(NexoColor.parse("&#FF5555☠ <bold>DESCONEXIÓN COBARDE:</bold> &#FFFFFF" + p.getName() + " &#AAAAAAevadió el combate y sus sistemas fueron purgados."));
        }
    }
}