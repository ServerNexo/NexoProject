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
    public static final String MENU_TITLE = "&#555555<bold>»</bold> &#FFAA00Mesa de Evolución Cénit";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaF = filler.getItemMeta();
        metaF.setDisplayName(" ");
        filler.setItemMeta(metaF);

        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Slot 13: El espacio vacío para que el jugador ponga su ítem
        inv.setItem(13, new ItemStack(Material.AIR));

        // Slot 22: Botón de Evolucionar
        ItemStack btn = new ItemStack(Material.NETHER_STAR);
        ItemMeta btnMeta = btn.getItemMeta();
        btnMeta.setDisplayName(serialize("&#00E5FF<bold>↑ INICIAR EVOLUCIÓN ↑</bold>"));
        btnMeta.setLore(Arrays.asList(
                serialize("&#AAAAAAColoca tu Arma o Herramienta arriba."),
                serialize("&#AAAAAAEl sistema calculará automáticamente"),
                serialize("&#AAAAAAel costo en &#FFAA00Fragmentos &#AAAAAAo &#55FF55Esencias&#AAAAAA.")
        ));
        btn.setItemMeta(btnMeta);
        inv.setItem(22, btn);

        player.openInventory(inv);
    }

    private static String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }
}