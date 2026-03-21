package me.nexo.clans.listeners;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ClanConnectionListener implements Listener {

    private final NexoClans plugin;

    public ClanConnectionListener(NexoClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Le damos 1 segundo al servidor para asegurar que NexoCore ya cargó al usuario de Supabase
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            // Pedimos el usuario a la caché de NexoCore
            NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

            // Si el usuario existe y pertenece a un clan, le pedimos al Manager que lo suba a la RAM
            if (user != null && user.hasClan()) {
                plugin.getClanManager().loadClanAsync(user.getClanId(), clan -> {
                    if (clan != null) {
                        // Opcional: Enviar un mensaje silencioso a la consola
                        // plugin.getLogger().info("Clan cargado/verificado en RAM para " + player.getName());
                    }
                });
            }

        }, 20L); // 20 ticks = 1 segundo
    }
}