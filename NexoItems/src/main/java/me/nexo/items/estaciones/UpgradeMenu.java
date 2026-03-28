package me.nexo.items.estaciones;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class UpgradeMenu {

    public static final String TITLE_PLAIN = "» Mesa de Evolución Cénit";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#ff00ffMesa de Evolución Cénit";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaF = filler.getItemMeta();
        metaF.setDisplayName(" ");
        filler.setItemMeta(metaF);

        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(13, new ItemStack(Material.AIR));

        ItemStack btn = new ItemStack(Material.NETHER_STAR);
        ItemMeta btnMeta = btn.getItemMeta();
        btnMeta.setDisplayName(serialize("&#00f5ff<bold>↑ INICIAR EVOLUCIÓN ↑</bold>"));
        btnMeta.setLore(Arrays.asList(
                serialize("&#1c0f2aColoca tu Arma o Herramienta arriba."),
                serialize("&#1c0f2aEl sistema calculará automáticamente"),
                serialize("&#1c0f2ael costo en &#ff00ffFragmentos &#1c0f2ao &#00f5ffEsencias&#1c0f2a.")
        ));
        btn.setItemMeta(btnMeta);
        inv.setItem(22, btn);

        player.openInventory(inv);
    }

    private static String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }
}