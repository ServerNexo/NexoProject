package me.nexo.dungeons.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class DungeonMenu {

    public static void openMainMenu(Player player, NexoDungeons plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.principal.titulo")));

        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        if (instancedMeta != null) {
            instancedMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.principal.instanciadas.titulo")));
            List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.principal.instanciadas.lore");
            instancedMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            instanced.setItemMeta(instancedMeta);
        }
        inv.setItem(11, instanced);

        ItemStack waves = new ItemStack(Material.IRON_SWORD);
        ItemMeta wavesMeta = waves.getItemMeta();
        if (wavesMeta != null) {
            wavesMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.principal.supervivencia.titulo")));
            List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.principal.supervivencia.lore");
            wavesMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            waves.setItemMeta(wavesMeta);
        }
        inv.setItem(13, waves);

        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        if (bossMeta != null) {
            bossMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.principal.amenazas-globales.titulo")));
            List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.principal.amenazas-globales.lore");
            bossMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            worldBoss.setItemMeta(bossMeta);
        }
        inv.setItem(15, worldBoss);

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}