package me.nexo.economy.bazar;

import me.nexo.core.utils.NexoColor;
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
        // 🌟 Ahora detectamos clics tanto en el menú principal como en el de Mis Órdenes
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

        // ==========================================
        // 🌟 NAVEGACIÓN (Fix de Bedrock de 3 ticks)
        // ==========================================
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

        // ==========================================
        // ❌ GESTIÓN Y CANCELACIÓN DE ÓRDENES (FASE 2)
        // ==========================================
        else if (action.equals("open_my_orders")) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BazaarMyOrdersMenu(plugin, player).openMenu();
            }, 3L);
        }
        else if (action.equals("cancel_order")) {
            // Leer el ID como INTEGER porque lo cambiamos a SERIAL autoincremental
            int orderId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "order_id"), PersistentDataType.INTEGER);
            player.closeInventory();
            plugin.getBazaarManager().cancelarOrden(player, orderId);
        }

        // ==========================================
        // ✍️ CREAR ÓRDENES CON INPUT DE CHAT DINÁMICO (FASE 2)
        // ==========================================
        else if (action.equals("create_buy_order") || action.equals("create_sell_order")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();

            // Identificar si está comprando o vendiendo
            String type = action.equals("create_buy_order") ? "BUY" : "SELL";

            // Iniciar sesión asíncrona de chat
            BazaarChatListener.activeSessions.put(player.getUniqueId(), new BazaarChatListener.OrderSession(itemId, type));

            // Guiar al usuario visualmente
            player.sendMessage(NexoColor.parse("&#fbd72b========================================"));
            player.sendMessage(NexoColor.parse("&#00fbff[NEXO] Has iniciado una cotización para: &#fbd72b" + itemId));
            player.sendMessage(NexoColor.parse("&#00fbffEscribe en el chat la &#a8ff78CANTIDAD TOTAL&#00fbff de ítems:"));
            player.sendMessage(NexoColor.parse("&#AAAAAA(O escribe 'cancelar' para abortar)"));
            player.sendMessage(NexoColor.parse("&#fbd72b========================================"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}