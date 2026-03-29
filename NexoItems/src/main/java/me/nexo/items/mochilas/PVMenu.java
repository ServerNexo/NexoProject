package me.nexo.items.mochilas;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
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

public class PVMenu implements InventoryHolder {

    private final NexoItems plugin;
    private final Player player;
    private Inventory inventory;

    public PVMenu(NexoItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.pv.titulo"));
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 36);
        this.inventory = Bukkit.createInventory(this, tamano, titulo);

        for (int i = 1; i <= 9; i++) {
            boolean tienePerm = (i == 1) || player.hasPermission("nexo.pv." + i);

            ItemStack pvItem = new ItemStack(tienePerm ? Material.ENDER_CHEST : Material.MINECART);
            ItemMeta meta = pvItem.getItemMeta();

            if (tienePerm) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.pv.mochila.desbloqueada.titulo").replace("%id%", String.valueOf(i))));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.pv.mochila.desbloqueada.lore");
                meta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "pv_action"), PersistentDataType.INTEGER, i);
            } else {
                meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.pv.mochila.bloqueada.titulo").replace("%id%", String.valueOf(i))));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.pv.mochila.bloqueada.lore");
                meta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            }
            pvItem.setItemMeta(meta);

            inventory.setItem(i + 8, pvItem);
        }

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaGlass = glass.getItemMeta();
        metaGlass.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(metaGlass);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) inventory.setItem(i, glass);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.pv.boton-volver")));
        backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, "open_hub");
        back.setItemMeta(backMeta);

        inventory.setItem(tamano - 5, back);

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}