package me.nexo.economy.listeners;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EconomyListener implements Listener {

    private final NexoEconomy plugin;

    public EconomyListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carga la cuenta del jugador en la RAM (o la crea si es nuevo) sin dar lag al server
        plugin.getEconomyManager().getAccountAsync(event.getPlayer().getUniqueId(), NexoAccount.AccountType.PLAYER).thenAccept(account -> {
            if (account != null) {
                plugin.getLogger().info("Billetera cargada para: " + event.getPlayer().getName());
            }
        });
    }
}