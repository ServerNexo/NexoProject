package me.nexo.colecciones.menu;

import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SlayerMenu {

    public static final String TITLE_MENU = "&#1c0f2a<bold>»</bold> &#00f5ffContratos de Eliminación (Slayer)";

    public static void abrirMenu(Player player, SlayerManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, TITLE_MENU));

        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {

            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name() + "</bold>"));

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aCódigo de Contrato: " + template.id()));
                lore.add(CrossplayUtils.parseCrossplay(player, " "));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aObjetivo: &#00f5ffEliminar " + template.requiredKills() + "x " + template.targetMob() + "s"));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aAmenaza Final: &#8b0000" + template.bossName()));
                lore.add(CrossplayUtils.parseCrossplay(player, " "));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff¡Haz clic para iniciar la Cacería!"));

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }
}