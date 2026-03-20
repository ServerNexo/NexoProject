package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.equals("§8Categorías de Colecciones") || title.startsWith("§8Colecciones: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Clic en Menú Principal
            if (title.equals("§8Categorías de Colecciones")) {
                String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                String catStr = org.bukkit.ChatColor.stripColor(displayName).toUpperCase();
                try {
                    CollectionCategory categoria = CollectionCategory.valueOf(catStr);
                    ColeccionesMenu.abrirSubMenu(player, plugin.getCollectionManager(), categoria);
                } catch (IllegalArgumentException ignored) {}
            }
            // Clic en Sub-Menú
            else if (title.startsWith("§8Colecciones: ")) {
                if (event.getCurrentItem().getType() == Material.ARROW) {
                    ColeccionesMenu.abrirMenuPrincipal(player);
                }
            }
        }
    }
}