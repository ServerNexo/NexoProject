package me.nexo.economy.bazar;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BazaarMyOrdersMenu implements InventoryHolder {

    private final NexoEconomy plugin;
    private final Player player;
    private Inventory inventory;

    public BazaarMyOrdersMenu(NexoEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        this.inventory = Bukkit.createInventory(this, 54, NexoColor.parse("&#FFAA00Mis Órdenes Activas"));

        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta lMeta = loading.getItemMeta();
        lMeta.displayName(NexoColor.parse("&#AAAAAAConsultando Base de Datos..."));
        loading.setItemMeta(lMeta);
        inventory.setItem(22, loading);

        fillBorders();
        addBackButton();
        player.openInventory(inventory);

        // Carga asíncrona para no dar lag al servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BazaarManager.ActiveOrderDTO> orders = plugin.getBazaarManager().getMisOrdenes(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.getOpenInventory().getTitle().contains("Mis Órdenes")) return;

                inventory.setItem(22, new ItemStack(Material.AIR)); // Quitar reloj

                int slot = 10;
                for (BazaarManager.ActiveOrderDTO order : orders) {
                    if (slot >= 44) break;

                    Material mat = Material.matchMaterial(order.itemId);
                    if (mat == null) mat = Material.STONE;

                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();

                    boolean isBuy = order.type.equals("BUY");
                    String color = isBuy ? "&#a8ff78" : "&#ff4b2b";
                    String tipo = isBuy ? "Compra" : "Venta";

                    meta.displayName(NexoColor.parse(color + "<bold>Orden de " + tipo + "</bold>"));

                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(NexoColor.parse("&#e0e0e0Ítem: &#fbd72b" + order.itemId));
                    lore.add(NexoColor.parse("&#e0e0e0Cantidad Restante: &#fbd72b" + order.amount));
                    lore.add(NexoColor.parse("&#e0e0e0Precio Cotizado C/U: &#fbd72b" + order.price + " 🪙"));
                    lore.add(NexoColor.parse(" "));
                    lore.add(NexoColor.parse("&#FF3366► Clic para CANCELAR orden"));

                    meta.lore(lore);
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
                    em.displayName(NexoColor.parse("&#ff4b2bNo tienes órdenes activas."));
                    empty.setItemMeta(em);
                    inventory.setItem(22, empty);
                }
            });
        });
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(NexoColor.parse("&#FF3366⬅ Volver"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_main");
        back.setItemMeta(meta);
        inventory.setItem(inventory.getSize() - 5, back);
    }

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.empty());
        glass.setItemMeta(meta);
        for (int j = 0; j < inventory.getSize(); j++) {
            if (inventory.getItem(j) == null || inventory.getItem(j).getType().isAir()) inventory.setItem(j, glass);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
}