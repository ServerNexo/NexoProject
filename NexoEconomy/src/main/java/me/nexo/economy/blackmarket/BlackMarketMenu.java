package me.nexo.economy.blackmarket;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BlackMarketMenu {

    public static final String TITLE_PLAIN = "» Mercado Negro";
    public static final String TITLE_MENU = "&#434343<bold>»</bold> &#8b008bMercado Negro";

    public static void open(Player player, NexoEconomy plugin) {
        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] Las sombras están vacías... El Mercader no está aquí."));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(TITLE_MENU));

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(serialize(" "));
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
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(serialize(" "));

                String color = bmItem.currency() == NexoAccount.Currency.GEMS ? "&#a8ff78" : "&#00fbff";
                String divisaNombre = bmItem.currency() == NexoAccount.Currency.GEMS ? "💎 Gemas" : "💧 Maná";

                lore.add(serialize("&#434343======================="));
                lore.add(serialize("&#434343Tarifa: " + color + bmItem.price() + " " + divisaNombre));
                lore.add(serialize("&#434343======================="));
                lore.add(serialize("&#fbd72b▶ Clic para pactar compra"));

                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slots[i], display);
        }

        player.openInventory(inv);
    }

    private static String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }
}