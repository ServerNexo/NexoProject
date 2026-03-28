package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
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

public class BazaarMenuListener implements Listener {

    private final NexoEconomy plugin;

    public BazaarMenuListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        boolean isBazaarMenu = event.getInventory().getHolder() instanceof BazaarMenu;
        boolean isMyOrdersMenu = event.getInventory().getHolder() instanceof BazaarMyOrdersMenu;

        if (!isBazaarMenu && !isMyOrdersMenu) return;
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

        if (action.equals("open_category")) {
            String cat = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMenu(plugin, player, BazaarMenu.MenuType.CATEGORY).openCategory(cat);
            }, 3L);
        } else if (action.equals("open_options")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMenu(plugin, player, BazaarMenu.MenuType.ITEM_OPTIONS).openItemOptions(itemId);
            }, 3L);
        } else if (action.equals("claim_deliveries")) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            plugin.getBazaarManager().reclamarBuzon(player);
        } else if (action.startsWith("back_")) {
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
        } else if (action.equals("open_my_orders")) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMyOrdersMenu(plugin, player).openMenu();
            }, 3L);
        } else if (action.equals("cancel_order")) {
            int orderId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "order_id"), PersistentDataType.INTEGER);
            player.closeInventory();
            plugin.getBazaarManager().cancelarOrden(player, orderId);
        } else if (action.equals("create_buy_order") || action.equals("create_sell_order")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();

            String type = action.equals("create_buy_order") ? "BUY" : "SELL";
            BazaarChatListener.activeSessions.put(player.getUniqueId(), new BazaarChatListener.OrderSession(itemId, type));

            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.divisor"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.solicitar-precio").replace("%item_id%", itemId));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.cantidad-fijada").replace("%amount%", "CANTIDAD TOTAL"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.valor-invalido"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.divisor"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}