package me.nexo.core;

import me.nexo.core.user.NexoUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final NexoCore plugin;

    public PlayerListener(NexoCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();

        // 🌟 1. Creamos el perfil temporal del usuario en base a tu clase NexoUser
        // (Nota: Si tu NexoUser pide más variables, agrégalas aquí adentro)
        // 🌟 1. Creamos el perfil temporal con Nivel 1 y 0 de XP en todo
        NexoUser usuario = new NexoUser(
                uuid,           // UUID
                p.getName(),    // Nombre
                1, 0,           // Nexo: Nivel 1, 0 XP
                1, 0,           // Combate: Nivel 1, 0 XP
                1, 0,           // Minería: Nivel 1, 0 XP
                1, 0            // Agricultura: Nivel 1, 0 XP
        );

        // 🌟 2. Lo guardamos en tu ultra-rápida caché de Caffeine
        plugin.getUserManager().addUserToCache(usuario);

        // (Aquí podrías añadir una llamada asíncrona a tu DatabaseManager para cargar stats de Supabase)
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // (Aquí podrías llamar a tu DatabaseManager para guardar los stats en Supabase antes de borrar la caché)

        // 🌟 Removemos al jugador de la RAM para evitar fugas de memoria
        plugin.getUserManager().removeUserFromCache(uuid);
    }
}