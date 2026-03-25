package me.nexo.war.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils; // 🌟 TRADUCTOR UNIVERSAL
import me.nexo.core.user.NexoUser;
import me.nexo.war.NexoWar;
import me.nexo.war.core.WarContract;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.UUID;

public class WarListener implements Listener {

    private final NexoWar plugin;

    public WarListener(NexoWar plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 🛡️ CONTROL DE CUPOS CROSS-PLAY
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWarCombat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victima && event.getDamager() instanceof Player atacante) {

            NexoUser uVictima = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(victima.getUniqueId());
            NexoUser uAtacante = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(atacante.getUniqueId());

            if (uVictima == null || !uVictima.hasClan() || uAtacante == null || !uAtacante.hasClan()) return;

            UUID clanVictima = uVictima.getClanId();
            UUID clanAtacante = uAtacante.getClanId();

            if (clanVictima.equals(clanAtacante)) return; // Fuego amigo anulado

            Optional<WarContract> guerraOpt = plugin.getWarManager().getGuerraEntre(clanAtacante, clanVictima);

            if (guerraOpt.isPresent() && guerraOpt.get().status() == WarContract.WarStatus.ACTIVE) {
                WarContract guerra = guerraOpt.get();

                boolean atacanteIsBedrock = FloodgateApi.getInstance().isFloodgatePlayer(atacante.getUniqueId());
                boolean isAtacanteClan = clanAtacante.equals(guerra.clanAtacante());

                // 🛡️ Validamos el Matchmaking y los Cupos
                if (!guerra.registrarParticipante(atacante.getUniqueId(), isAtacanteClan, atacanteIsBedrock)) {
                    event.setCancelled(true);

                    // Empuje cinético (Lo rebota hacia atrás)
                    Vector pushback = atacante.getLocation().getDirection().multiply(-1).setY(0.3);
                    atacante.setVelocity(pushback);

                    String plataforma = atacanteIsBedrock ? "Táctil/Consola" : "PC (Java)";
                    CrossplayUtils.sendActionBar(atacante, "&#FF5555🛡️ Protocolo Nexo: El escuadrón de " + plataforma + " de tu Sindicato ya está lleno.");
                }
            }
        }
    }

    // ==========================================
    // 💀 RASTREO DE BAJAS
    // ==========================================
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victima = event.getEntity();
        Player asesino = victima.getKiller();

        if (asesino == null) return;

        NexoUser uVictima = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(victima.getUniqueId());
        NexoUser uAsesino = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(asesino.getUniqueId());

        if (uVictima == null || !uVictima.hasClan()) return;
        if (uAsesino == null || !uAsesino.hasClan()) return;

        UUID clanVictima = uVictima.getClanId();
        UUID clanAsesino = uAsesino.getClanId();

        // Verificamos si sus clanes están matándose formalmente
        Optional<WarContract> guerraOpt = plugin.getWarManager().getGuerraEntre(clanAsesino, clanVictima);

        if (guerraOpt.isPresent() && guerraOpt.get().status() == WarContract.WarStatus.ACTIVE) {
            plugin.getWarManager().registrarBaja(guerraOpt.get(), clanAsesino, asesino, victima);
        }
    }
}