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
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // 🌟 CARGA DE DATOS REAL (Supabase)
        // Esto buscará tu info en la base de datos. Si no existes, te creará.
        // Si ya existes, cargará tus niveles, XP y tu CLAN a la RAM.
        plugin.getDatabaseManager().cargarJugador(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        // 🌟 GUARDADO DE DATOS (Supabase)
        // Esto tomará tus stats de la RAM, las subirá a Supabase,
        // y luego el mismo método se encarga de borrarte de la caché para liberar memoria.
        plugin.getDatabaseManager().guardarJugador(p);
    }
}