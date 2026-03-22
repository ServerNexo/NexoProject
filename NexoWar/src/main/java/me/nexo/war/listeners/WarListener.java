package me.nexo.war.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.war.NexoWar;
import me.nexo.war.core.WarContract;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;
import java.util.UUID;

public class WarListener implements Listener {

    private final NexoWar plugin;

    public WarListener(NexoWar plugin) {
        this.plugin = plugin;
    }

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