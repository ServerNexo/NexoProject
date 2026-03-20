package me.nexo.colecciones.menu;

import me.nexo.colecciones.slayers.SlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SlayerMenu {

    public static void abrirMenu(Player player, SlayerManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Misiones de Slayer");

        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {

            // Intentamos buscar la cabeza del mob o un ítem que lo represente
            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c§l" + template.name());

                List<String> lore = new ArrayList<>();
                lore.add("§8ID: " + template.id());
                lore.add("");
                lore.add("§7Objetivo: §cMatar " + template.requiredKills() + " " + template.targetMob() + "s");
                lore.add("§7Jefe Final: §4" + template.bossName());
                lore.add("");
                lore.add("§a¡Haz clic para iniciar la Cacería!");

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }
}