package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class BazaarMyOrdersMenu extends NexoMenu {

    private final NexoEconomy plugin;

    public BazaarMyOrdersMenu(Player player, NexoEconomy plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.titulo");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta lMeta = loading.getItemMeta();
        lMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.cargando")));
        loading.setItemMeta(lMeta);
        inventory.setItem(22, loading);

        addBackButton();

        // 🚀 Carga asíncrona segura a la base de datos
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BazaarManager.ActiveOrderDTO> orders = plugin.getBazaarManager().getMisOrdenes(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                // 🌟 PROTECCIÓN ANTI-CRASH: Verificamos directamente el objeto inventario, adiós al PlainTextComponentSerializer
                if (player.getOpenInventory().getTopInventory() != inventory) return;

                inventory.setItem(22, new ItemStack(Material.AIR));

                int slot = 10;
                for (BazaarManager.ActiveOrderDTO order : orders) {
                    if (slot >= 44) break;

                    Material mat = Material.matchMaterial(order.itemId);
                    if (mat == null) mat = Material.STONE;

                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();

                    boolean isBuy = order.type.equals("BUY");
                    String titulo = isBuy ? plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.orden.compra") : plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.orden.venta");
                    meta.displayName(CrossplayUtils.parseCrossplay(player, titulo));

                    List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.bazar.mis-ordenes-menu.orden.lore");
                    List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                            .map(line -> CrossplayUtils.parseCrossplay(player, line
                                    .replace("%item_id%", order.itemId)
                                    .replace("%amount%", String.valueOf(order.amount))
                                    .replace("%price%", order.price.toString())))
                            .collect(Collectors.toList());
                    meta.lore(lore);

                    // 🌟 MAGIA: Incrustamos la acción de cancelar en el ítem (PDC)
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "cancel_order");
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "order_id"), PersistentDataType.INTEGER, order.id);
                    item.setItemMeta(meta);

                    inventory.setItem(slot, item);

                    slot++;
                    if (slot == 17 || slot == 26 || slot == 35) slot += 2;
                }

                if (orders.isEmpty()) {
                    ItemStack empty = new ItemStack(Material.BARRIER);
                    ItemMeta em = empty.getItemMeta();
                    em.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.vacio")));
                    empty.setItemMeta(em);
                    inventory.setItem(22, empty);
                }

                setFillerGlass(); // Rematar cualquier hueco
            });
        });
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.boton-volver")));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_main");
        back.setItemMeta(meta);
        inventory.setItem(getSlots() - 5, back);
    }

    // 🌟 GESTOR DE CLICS BLINDADO
    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Cero robos o arrastres por error

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if (action.equals("cancel_order")) {
            int orderId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "order_id"), PersistentDataType.INTEGER);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            // 🌟 BEDROCK FIX: Cierra el menú al instante, cancela la orden, y reabre 3 ticks después
            player.closeInventory();
            plugin.getBazaarManager().cancelarOrden(player, orderId);
            Bukkit.getScheduler().runTaskLater(plugin, () -> new BazaarMyOrdersMenu(player, plugin).open(), 3L);

        } else if (action.equals("back_main")) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> new BazaarMenu(player, plugin).open(), 3L);
        }
    }
}