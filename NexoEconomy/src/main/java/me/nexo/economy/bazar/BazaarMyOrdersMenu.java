package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class BazaarMyOrdersMenu implements InventoryHolder {

    private final NexoEconomy plugin;
    private final Player player;
    private Inventory inventory;

    public BazaarMyOrdersMenu(NexoEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        this.inventory = Bukkit.createInventory(this, 54, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.titulo")));

        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta lMeta = loading.getItemMeta();
        lMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.mis-ordenes-menu.cargando")));
        loading.setItemMeta(lMeta);
        inventory.setItem(22, loading);

        fillBorders();
        addBackButton();
        player.openInventory(inventory);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BazaarManager.ActiveOrderDTO> orders = plugin.getBazaarManager().getMisOrdenes(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                // 🌟 CORRECCIÓN: Leemos el componente del título usando el serializador nativo de Kyori
                String tituloPlano = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
                if (!tituloPlano.contains("Mis Órdenes")) return;

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
            });
        });
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.bazar.boton-volver")));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_main");
        back.setItemMeta(meta);
        inventory.setItem(inventory.getSize() - 5, back);
    }

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(meta);
        for (int j = 0; j < inventory.getSize(); j++) {
            if (inventory.getItem(j) == null || inventory.getItem(j).getType().isAir()) inventory.setItem(j, glass);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}