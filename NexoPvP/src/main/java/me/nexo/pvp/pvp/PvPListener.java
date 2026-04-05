package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.pvp.config.ConfigManager;
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

/**
 * 🏛️ NexoPvP - Listener de Combate (Arquitectura Enterprise)
 * Textos centralizados y Dependencias Inyectadas.
 */
public class PvPListener implements Listener {

    private final PvPManager manager;
    private final ConfigManager configManager; // 💡 PILAR 2

    // 💉 PILAR 3: Inyección
    @Inject
    public PvPListener(PvPManager manager, ConfigManager configManager) {
        this.manager = manager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDañoJugadores(EntityDamageByEntityEvent event) {

        Player tempAtacante = null;
        if (event.getDamager() instanceof Player p) tempAtacante = p;
        else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) tempAtacante = p;

        final Player atacante = tempAtacante;

        if (atacante != null && event.getEntity() instanceof Player victima) {
            if (atacante.equals(victima)) return;

            NexoAPI.getServices().get(ClaimManager.class).ifPresent(claimManager -> {
                me.nexo.protections.core.ProtectionStone stone = claimManager.getStoneAt(victima.getLocation());
                if (stone != null && !stone.getFlag("pvp")) {
                    boolean ignorarProteccion = false;
                    if (Bukkit.getPluginManager().isPluginEnabled("NexoWar")) {
                        ignorarProteccion = me.nexo.war.NexoWar.getPlugin(me.nexo.war.NexoWar.class).getWarManager().estanEnGuerraActiva(atacante.getUniqueId(), victima.getUniqueId());
                    }
                    if (!ignorarProteccion) {
                        CrossplayUtils.sendMessage(atacante, configManager.getMessages().mensajes().pvp().bloqueoArmamento());
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

            // 💡 TYPE-SAFE TEXT
            CrossplayUtils.sendMessage(asesino, configManager.getMessages().mensajes().pvp().objetivoNeutralizado()
                    .replace("%victima%", victima.getName()));

            int rachaVictima = manager.rachaAsesinatos.getOrDefault(idVictima, 0);

            if (rachaVictima >= 3) {
                Bukkit.broadcast(NexoColor.parse(configManager.getMessages().mensajes().pvp().cazarrecompensasGlobal()
                        .replace("%asesino%", asesino.getName())
                        .replace("%victima%", victima.getName())));

                CrossplayUtils.sendMessage(asesino, configManager.getMessages().mensajes().pvp().bountyReclamado());

                manager.puntosHonor.put(idAsesino, honorActual + 5);
                asesino.getInventory().addItem(new ItemStack(Material.DIAMOND));
                asesino.playSound(asesino.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            int rachaAsesino = manager.rachaAsesinatos.getOrDefault(idAsesino, 0) + 1;
            manager.rachaAsesinatos.put(idAsesino, rachaAsesino);

            if (rachaAsesino == 3) {
                Bukkit.broadcast(NexoColor.parse(configManager.getMessages().mensajes().pvp().rachaTresGlobal()
                        .replace("%asesino%", asesino.getName())));
            } else if (rachaAsesino > 3) {
                Bukkit.broadcast(NexoColor.parse(configManager.getMessages().mensajes().pvp().rachaMayorGlobal()
                        .replace("%asesino%", asesino.getName())
                        .replace("%kills%", String.valueOf(rachaAsesino))));
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
            Bukkit.broadcast(NexoColor.parse(configManager.getMessages().mensajes().pvp().desconexionCobarde()
                    .replace("%jugador%", p.getName())));
        }
    }
}