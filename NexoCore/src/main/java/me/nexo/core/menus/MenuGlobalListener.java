package me.nexo.core.menus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuGlobalListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // Si no hizo clic en un inventario, ignorar
        if (event.getClickedInventory() == null) return;

        // Obtenemos al dueño del inventario
        InventoryHolder holder = event.getClickedInventory().getHolder();

        // 🌟 LA MAGIA: ¿El inventario es parte de nuestro nuevo sistema NexoMenu?
        if (holder instanceof NexoMenu nexoMenu) {

            // 1. Cancelamos el evento para que no puedan robarse los ítems del menú
            event.setCancelled(true);

            // 2. Le pasamos la pelota al menú específico que el jugador tiene abierto
            nexoMenu.handleMenu(event);
        }
    }
}