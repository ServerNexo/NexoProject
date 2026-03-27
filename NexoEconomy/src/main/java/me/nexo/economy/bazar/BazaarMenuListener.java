package me.nexo.economy.bazar;

import me.nexo.economy.NexoEconomy;
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

import java.math.BigDecimal;

public class BazaarMenuListener implements Listener {

    private final NexoEconomy plugin;

    public BazaarMenuListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BazaarMenu menu)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().equals(event.getWhoClicked().getInventory())) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        // 🌟 NAVEGACIÓN (Fix de Bedrock de 3 ticks)
        if (action.equals("open_category")) {
            String cat = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMenu(plugin, player, BazaarMenu.MenuType.CATEGORY).openCategory(cat);
            }, 3L);
        }
        else if (action.equals("open_options")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMenu(plugin, player, BazaarMenu.MenuType.ITEM_OPTIONS).openItemOptions(itemId);
            }, 3L);
        }
        else if (action.equals("claim_deliveries")) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            plugin.getBazaarManager().reclamarBuzon(player);
        }
        // 🌟 CREAR ORDENES (Por ahora simula 64 ítems a precio fijo. Puedes conectarlo a chat/yunque en el futuro)
        else if (action.equals("create_buy_order")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();
            // EJEMPLO: Comprar 64 a 10.5 monedas cada uno.
            plugin.getBazaarManager().crearOrdenCompra(player, itemId, 64, new BigDecimal("10.50"));
        }
        else if (action.equals("create_sell_order")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();
            // EJEMPLO: Vender 64 a 10 monedas cada uno.
            plugin.getBazaarManager().crearOrdenVenta(player, itemId, 64, new BigDecimal("10.00"));
        }
        else if (action.startsWith("back_")) {
            String target = action.replace("back_", "");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.equals("main")) {
                    new BazaarMenu(plugin, player, BazaarMenu.MenuType.MAIN).openMain();
                } else if (target.startsWith("cat_")) {
                    new BazaarMenu(plugin, player, BazaarMenu.MenuType.CATEGORY).openCategory(target.replace("cat_", ""));
                }
            }, 3L);
        }
    }
}