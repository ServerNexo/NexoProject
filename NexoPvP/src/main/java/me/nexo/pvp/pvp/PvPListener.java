package me.nexo.pvp.pvp;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.managers.ClaimManager;
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

        // 🌟 CORRECCIÓN: Usamos una variable temporal para determinar quién hizo el daño
        Player tempAtacante = null;
        if (event.getDamager() instanceof Player p) tempAtacante = p;
        else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) tempAtacante = p;

        // 🌟 CORRECCIÓN: Creamos al Atacante final (Esta es la llave para que el Lambda no crashee)
        final Player atacante = tempAtacante;

        if (atacante != null && event.getEntity() instanceof Player victima) {
            if (atacante.equals(victima)) return;

            // Ahora 'atacante' es seguro para usarse dentro de este bloque Lambda
            NexoAPI.getServices().get(ClaimManager.class).ifPresent(claimManager -> {
                me.nexo.protections.core.ProtectionStone stone = claimManager.getStoneAt(victima.getLocation());
                if (stone != null && !stone.getFlag("pvp")) {
                    boolean ignorarProteccion = false;
                    if (Bukkit.getPluginManager().isPluginEnabled("NexoWar")) {
                        ignorarProteccion = me.nexo.war.NexoWar.getPlugin(me.nexo.war.NexoWar.class).getWarManager().estanEnGuerraActiva(atacante.getUniqueId(), victima.getUniqueId());
                    }
                    if (!ignorarProteccion) {
                        atacante.sendMessage(NexoColor.parse("&#8b0000[!] Bloqueo de Armamento: &#1c0f2aEl objetivo se encuentra en una zona neutral."));
                        event.setCancelled(true);
                    }
                }
            });

            if (event.isCancelled()) return;

            if (!manager.tienePvP(atacante) || !manager.tienePvP(victima)) {
                event.setCancelled(true);
                return;
            }

            manager.marcarEnCombate(atacante, victima);
            event.setDamage(event.getDamage() * 0.40);
        }
    }

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
            asesino.sendMessage(NexoColor.parse("&#ff00ff⚔ <bold>OBJETIVO NEUTRALIZADO:</bold> &#1c0f2a" + victima.getName() + " &#00f5ff(+1 Honor)"));

            int rachaVictima = manager.rachaAsesinatos.getOrDefault(idVictima, 0);

            if (rachaVictima >= 3) {
                Bukkit.broadcast(NexoColor.parse("&#00f5ff<bold>[CAZARRECOMPENSAS]</bold> &#1c0f2a" + asesino.getName() + " &#1c0f2aha cobrado el contrato por la cabeza de &#8b0000" + victima.getName() + "&#1c0f2a!"));
                asesino.sendMessage(NexoColor.parse("&#00f5ff[💎] <bold>Bounty Reclamado:</bold> &#1c0f2aTransferencia de +5 Honor y recurso primario completada."));
                manager.puntosHonor.put(idAsesino, honorActual + 5);
                asesino.getInventory().addItem(new ItemStack(Material.DIAMOND));
                asesino.playSound(asesino.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            int rachaAsesino = manager.rachaAsesinatos.getOrDefault(idAsesino, 0) + 1;
            manager.rachaAsesinatos.put(idAsesino, rachaAsesino);

            if (rachaAsesino == 3) {
                Bukkit.broadcast(NexoColor.parse("&#8b0000<bold>[OBJETIVO PRIORITARIO]</bold> &#1c0f2a" + asesino.getName() + " &#1c0f2aestá en racha letal (3 Kills). ¡Contrato de caza emitido!"));
            } else if (rachaAsesino > 3) {
                Bukkit.broadcast(NexoColor.parse("&#8b0000<bold>[AMENAZA NIVEL OMEGA]</bold> &#1c0f2a" + asesino.getName() + " &#1c0f2aha alcanzado " + rachaAsesino + " Kills consecutivas!"));
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
            Bukkit.broadcast(NexoColor.parse("&#8b0000☠ <bold>DESCONEXIÓN COBARDE:</bold> &#1c0f2a" + p.getName() + " &#1c0f2aevadió el combate y sus sistemas fueron purgados."));
        }
    }
}