package me.nexo.colecciones.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Si el inventario se llama "Mis Colecciones", cancelamos el clic
        if (event.getView().getTitle().equals("§8Mis Colecciones")) {
            event.setCancelled(true);
        }
    }
}