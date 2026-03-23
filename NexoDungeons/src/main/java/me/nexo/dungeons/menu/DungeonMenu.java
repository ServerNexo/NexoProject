package me.nexo.dungeons.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class DungeonMenu {

    public static final String MENU_TITLE = "§8⚔ §lPortal de Desafíos";

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        // 🏰 OPCIÓN 1: Instancias Privadas (Slot 11)
        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        instancedMeta.setDisplayName("§b§lMazmorras Instanciadas");
        instancedMeta.setLore(Arrays.asList(
                "§7Aventúrate en fortalezas generadas",
                "§7exclusivamente para ti y tu grupo.",
                " ",
                "§e✦ Puzzles interactivos",
                "§e✦ Sin interrupciones de otros",
                " ",
                "§a¡Haz clic para ver expediciones!"
        ));
        instanced.setItemMeta(instancedMeta);
        inv.setItem(11, instanced);

        // ⚔️ OPCIÓN 2: Arenas de Oleadas (Slot 13)
        // ⚔️ OPCIÓN 2: Arenas de Oleadas (Slot 13)
        ItemStack waves = new ItemStack(Material.IRON_SWORD); // Usamos IRON_SWORD que es 100% nativo
        ItemMeta wavesMeta = waves.getItemMeta();
        wavesMeta.setDisplayName("§c§lArenas de Supervivencia");
        wavesMeta.setLore(Arrays.asList(
                "§7Resiste interminables oleadas de",
                "§7monstruos cada vez más letales.",
                " ",
                "§c✦ Dificultad Exponencial",
                "§c✦ Checkpoints cada 5 rondas",
                " ",
                "§a¡Haz clic para hacer cola!"
        ));
        waves.setItemMeta(wavesMeta);
        inv.setItem(13, waves);

        // 🐉 OPCIÓN 3: Bosses del Mundo (Slot 15)
        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        bossMeta.setDisplayName("§5§lBosses del Mundo");
        bossMeta.setLore(Arrays.asList(
                "§7Información sobre los altares",
                "§7de invocación pública.",
                " ",
                "§d✦ Recompensas por daño (Top 3)",
                "§d✦ Botín instanciado (Anti-Robo)",
                " ",
                "§e¡Haz clic para ver ubicaciones!"
        ));
        worldBoss.setItemMeta(bossMeta);
        inv.setItem(15, worldBoss);

        // Rellenar vacíos con cristal negro para que se vea elegante
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}