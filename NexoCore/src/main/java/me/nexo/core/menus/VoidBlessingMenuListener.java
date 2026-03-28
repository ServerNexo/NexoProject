package me.nexo.core.menus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class VoidBlessingMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Bloqueo instantáneo si la interfaz pertenece al VoidBlessingMenu
        if (event.getInventory().getHolder() instanceof VoidBlessingMenu) {
            event.setCancelled(true);
        }
    }
}