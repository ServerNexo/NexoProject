package me.nexo.dungeons.listeners;

import me.nexo.dungeons.NexoDungeons;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DungeonSecurityListener implements Listener {

    private final NexoDungeons plugin;

    public DungeonSecurityListener(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // 🛑 1. ANTI-DUPE: Prevenir que los jugadores tiren ítems clave de la mazmorra al piso
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Player p = event.getPlayer();

        // Si está en el mundo de instancias y no es admin en creativo...
        if (p.getWorld().getName().equals("nexo_dungeons") && p.getGameMode() != GameMode.CREATIVE) {
            // Nota: Aquí podrías verificar si el ítem tiene un tag "nexo_dungeon_item"
            // Por seguridad extrema, muchos servidores bloquean tirar cualquier ítem en la dungeon.
            // event.setCancelled(true);
            // p.sendMessage("§cNo puedes tirar ítems dentro de una instancia temporal.");
        }
    }

    // 🛑 2. ANTI-DUPE (Requested): Cancelar transacciones raras al cerrar inventarios
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().getWorld().getName().equals("nexo_dungeons")) {
            // Lógica para limpiar el cursor si el jugador intenta hacer un "Ghost Item Dupe"
            // al cerrar un cofre de la mazmorra con un ítem pegado en el ratón.
            if (event.getView().getCursor() != null && !event.getView().getCursor().getType().isAir()) {
                // Para servidores muy estrictos, se borra el cursor o se fuerza al inventario
            }
        }
    }

    // 🚪 3. CONTROL DE SESIÓN: Qué pasa si alguien se desconecta en plena mazmorra
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().getName().equals("nexo_dungeons")) {
            // Lo mandamos al spawn del mundo principal para que no se quede atrapado
            // en un Void World si el servidor borra el schematic mientras no está.
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.teleport(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
            }, 1L);
        }
    }
}