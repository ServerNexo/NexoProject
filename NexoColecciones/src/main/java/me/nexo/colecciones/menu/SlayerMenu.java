package me.nexo.colecciones.menu;

import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SlayerMenu {

    // 🎨 CONSTANTE PARA EL LISTENER
    public static final String TITLE_MENU = "&#434343<bold>»</bold> &#ff4b2bContratos de Eliminación (Slayer)";

    public static void abrirMenu(Player player, SlayerManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(TITLE_MENU));

        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {

            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(NexoColor.parse("&#ff4b2b<bold>" + template.name() + "</bold>"));

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(NexoColor.parse("&#434343Código de Contrato: " + template.id()));
                lore.add(NexoColor.parse(" "));
                lore.add(NexoColor.parse("&#434343Objetivo: &#ff4b2bEliminar " + template.requiredKills() + "x " + template.targetMob() + "s"));
                lore.add(NexoColor.parse("&#434343Amenaza Final: &#8b0000" + template.bossName()));
                lore.add(NexoColor.parse(" "));
                lore.add(NexoColor.parse("&#a8ff78¡Haz clic para iniciar la Cacería!"));

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }
}