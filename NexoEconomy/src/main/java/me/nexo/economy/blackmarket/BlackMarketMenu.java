package me.nexo.economy.blackmarket;

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

public class BlackMarketMenu {

    public static void open(Player player, NexoEconomy plugin) {
        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.sendMessage("§cLas sombras están vacías... El Mercader no está aquí.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§5🌑 Mercado Negro");

        // Rellenar con cristal negro para darle ambiente
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Colocar los 3 ítems aleatorios de la rotación actual
        List<BlackMarketItem> stock = plugin.getBlackMarketManager().getCurrentStock();
        int[] slots = {11, 13, 15}; // Posiciones centrales

        for (int i = 0; i < stock.size() && i < slots.length; i++) {
            BlackMarketItem bmItem = stock.get(i);

            // Clonamos para no modificar el original al añadirle el lore del precio
            ItemStack display = bmItem.displayItem().clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            String color = bmItem.currency() == NexoAccount.Currency.GEMS ? "§a" : "§b";
            String divisaNombre = bmItem.currency() == NexoAccount.Currency.GEMS ? "💎 Gemas" : "💧 Maná";

            lore.add("§8=======================");
            lore.add("§7Costo: " + color + bmItem.price() + " " + divisaNombre);
            lore.add("§8=======================");
            lore.add("§e▶ Clic para comprar");

            meta.setLore(lore);
            display.setItemMeta(meta);

            inv.setItem(slots[i], display);
        }

        player.openInventory(inv);
    }
}