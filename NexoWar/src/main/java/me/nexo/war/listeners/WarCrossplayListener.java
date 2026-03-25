package me.nexo.war.listeners;

import me.nexo.core.utils.NexoColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.geysermc.floodgate.api.FloodgateApi;

public class WarCrossplayListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInterPlatformCombat(EntityDamageByEntityEvent event) {
        // Solo nos interesa si el combate es Jugador vs Jugador
        if (event.getEntity() instanceof Player victima && event.getDamager() instanceof Player atacante) {

            // Verificamos la plataforma de origen mediante los registros de Floodgate
            boolean isBedrockVictim = FloodgateApi.getInstance().isFloodgatePlayer(victima.getUniqueId());
            boolean isBedrockAttacker = FloodgateApi.getInstance().isFloodgatePlayer(atacante.getUniqueId());

            // Si las plataformas no coinciden, se bloquea el daño
            if (isBedrockVictim != isBedrockAttacker) {
                event.setCancelled(true);

                // Alerta táctica enviada a la ActionBar para no saturar el chat
                atacante.sendActionBar(NexoColor.parse("&#FF5555🛡️ Protocolo Nexo: Combate prohibido entre diferentes plataformas."));
            }
        }
    }
}