package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final NexoColecciones plugin;

    public MenuListener(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Solo respondemos si el inventario es nuestro Grimorio
        if (!(event.getInventory().getHolder() instanceof ColeccionesMenu menu)) return;
        event.setCancelled(true); // Bloquear que muevan o roben cristales

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().equals(event.getWhoClicked().getInventory())) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        // Si no tiene llave de acción, ignoramos el clic (Ej: Clic en cristal negro)
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        // 🌟 BEDROCK FIX: Cerramos inventario, esperamos 3 ticks (3L) y abrimos el nuevo para evitar bugs
        if (action.equals("open_category")) {
            String catId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.CATEGORY).openCategory(catId);
            }, 3L);
        }
        else if (action.equals("open_item")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.ITEM_TIERS).openItemTiers(itemId);
            }, 3L);
        }
        else if (action.equals("claim_tier")) {
            Integer tierNivel = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "tier_level"), PersistentDataType.INTEGER);
            if (tierNivel != null) {
                // Reclamar la recompensa en el Motor Central
                plugin.getCollectionManager().reclamarRecompensa(player, menu.getItemId(), tierNivel);

                // Refrescar el menú actual (Dopamina visual de amarillo a verde)
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.ITEM_TIERS).openItemTiers(menu.getItemId());
                }, 3L);
            }
        }
        else if (action.equals("show_top")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);

            // Llama a la Base de Datos para sacar el Top 5
            plugin.getCollectionManager().calcularTopAsync(player, itemId);
        }
        else if (action.startsWith("back_")) {
            String target = action.replace("back_", "");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.equals("main")) {
                    new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.MAIN).openMain();
                } else {
                    new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.CATEGORY).openCategory(target);
                }
            }, 3L);
        }
    }
}