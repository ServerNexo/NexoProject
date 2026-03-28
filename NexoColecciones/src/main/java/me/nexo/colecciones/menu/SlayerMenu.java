package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class SlayerMenu {

    public static void abrirMenu(Player player, NexoColecciones plugin) {
        SlayerManager manager = plugin.getSlayerManager();
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.slayer.titulo")));

        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {
            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name() + "</bold>"));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.slayer.item-lore");
                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line
                                .replace("%id%", template.id())
                                .replace("%kills%", String.valueOf(template.requiredKills()))
                                .replace("%mob%", template.targetMob())
                                .replace("%boss_name%", template.bossName())))
                        .collect(Collectors.toList());
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }
        player.openInventory(inv);
    }
}