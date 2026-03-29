package me.nexo.items.estaciones;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

public class UpgradeMenu {

    public static void open(Player player, NexoItems plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.upgrade.titulo")));

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaF = filler.getItemMeta();
        metaF.displayName(CrossplayUtils.parseCrossplay(player, " "));
        filler.setItemMeta(metaF);

        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(13, new ItemStack(Material.AIR));

        ItemStack btn = new ItemStack(Material.NETHER_STAR);
        ItemMeta btnMeta = btn.getItemMeta();
        btnMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.upgrade.boton.titulo")));
        btnMeta.lore(plugin.getConfigManager().getMessages().getStringList("menus.upgrade.boton.lore").stream()
                .map(line -> CrossplayUtils.parseCrossplay(player, line))
                .collect(Collectors.toList()));
        btn.setItemMeta(btnMeta);
        inv.setItem(22, btn);

        player.openInventory(inv);
    }
}