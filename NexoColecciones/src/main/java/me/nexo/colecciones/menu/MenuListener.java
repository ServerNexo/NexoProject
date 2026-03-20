package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.slayers.SlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // 🌟 1. MENÚS DE COLECCIONES
        if (title.equals("§8Categorías de Colecciones") || title.startsWith("§8Colecciones: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Clic en Menú Principal
            if (title.equals("§8Categorías de Colecciones")) {
                String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                String catStr = ChatColor.stripColor(displayName).toUpperCase();
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

        // 🌟 2. NUEVO: MENÚ DE SLAYERS
        else if (title.equals("§8Misiones de Slayer")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Le quitamos los colores al nombre para poder compararlo
            String slayerName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            Player player = (Player) event.getWhoClicked();
            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Buscamos el ID correcto basado en el nombre de la plantilla
            for (SlayerManager.SlayerTemplate template : plugin.getSlayerManager().getTemplates().values()) {
                String cleanTemplateName = ChatColor.stripColor(template.name());

                if (cleanTemplateName.equals(slayerName)) {
                    player.closeInventory();
                    plugin.getSlayerManager().iniciarSlayer(player, template.id());
                    break;
                }
            }
        }
    }
}