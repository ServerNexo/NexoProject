package me.nexo.economy.blackmarket;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlackMarketMenu {

    public static void open(Player player, NexoEconomy plugin) {
        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.mercader-no-esta"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.blackmarket.titulo")));

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        List<BlackMarketItem> stock = plugin.getBlackMarketManager().getCurrentStock();
        int[] slots = {11, 13, 15};

        for (int i = 0; i < stock.size() && i < slots.length; i++) {
            BlackMarketItem bmItem = stock.get(i);

            ItemStack display = bmItem.displayItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.blackmarket.item-lore");
                String color = bmItem.currency() == NexoAccount.Currency.GEMS ? "&#00f5ff" : "&#ff00ff";
                String divisaNombre = bmItem.currency() == NexoAccount.Currency.GEMS ? "💎 Gemas" : "💧 Maná";

                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line
                                .replace("%color%", color)
                                .replace("%price%", bmItem.price().toString())
                                .replace("%currency%", divisaNombre)))
                        .collect(Collectors.toList());

                if (meta.hasLore()) {
                    List<net.kyori.adventure.text.Component> originalLore = meta.lore();
                    if (originalLore != null) {
                        originalLore.addAll(lore);
                        meta.lore(originalLore);
                    } else {
                        meta.lore(lore);
                    }
                } else {
                    meta.lore(lore);
                }
                display.setItemMeta(meta);
            }
            inv.setItem(slots[i], display);
        }

        player.openInventory(inv);
    }
}