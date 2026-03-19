package me.nexo.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final NexoCore plugin;

    public PlayerListener(NexoCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        // Solo cargamos la Base de Datos
        plugin.getDatabaseManager().cargarJugador(jugador);
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        Player jugador = event.getPlayer();
        // Solo guardamos en la Base de Datos
        plugin.getDatabaseManager().guardarJugador(jugador);
    }
}