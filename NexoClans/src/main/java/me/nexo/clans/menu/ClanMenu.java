package me.nexo.clans.menu;

import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClanMenu {

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user) {
        // Creamos un inventario de 4 filas (36 espacios)
        Inventory inv = Bukkit.createInventory(null, 36, "§8§l» §eTu Clan");

        // ⬛ Cristal de fondo para que se vea elegante
        ItemStack panel = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta panelMeta = panel.getItemMeta();
        panelMeta.setDisplayName(" ");
        panel.setItemMeta(panelMeta);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, panel);
        }

        // 🏛️ Monolito (Centro - Slot 13)
        ItemStack monolith = new ItemStack(Material.BEACON);
        ItemMeta monoMeta = monolith.getItemMeta();
        monoMeta.setDisplayName("§6§l🏛️ Monolito de " + clan.getName());
        List<String> monoLore = new ArrayList<>();
        monoLore.add("§7Tag: §e[" + clan.getTag() + "]");
        monoLore.add("§7Nivel actual: §a" + clan.getMonolithLevel());
        monoLore.add("§7Experiencia: §b" + clan.getMonolithExp());
        monoMeta.setLore(monoLore);
        monolith.setItemMeta(monoMeta);
        inv.setItem(13, monolith);

        // 💰 Banco (Izquierda - Slot 11)
        ItemStack bank = new ItemStack(Material.EMERALD);
        ItemMeta bankMeta = bank.getItemMeta();
        bankMeta.setDisplayName("§a§l💰 Banco del Clan");
        List<String> bankLore = new ArrayList<>();
        bankLore.add("§7Balance: §2$" + clan.getBankBalance());
        bankLore.add("");
        bankLore.add("§e▶ (Depósitos en construcción) 🏗️");
        bankMeta.setLore(bankLore);
        bank.setItemMeta(bankMeta);
        inv.setItem(11, bank);

        // 🛡️ Fuego Amigo (Derecha - Slot 15)
        ItemStack ff = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.SHIELD);
        ItemMeta ffMeta = ff.getItemMeta();
        ffMeta.setDisplayName("§c§l⚔️ Fuego Amigo");
        List<String> ffLore = new ArrayList<>();
        ffLore.add("§7Estado: " + (clan.isFriendlyFire() ? "§cACTIVADO" : "§aDESACTIVADO"));
        ffLore.add("");
        if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
            ffLore.add("§e▶ Clic para alternar");
        } else {
            ffLore.add("§8Solo Líderes y Oficiales");
            ffLore.add("§8pueden cambiar esto.");
        }
        ffMeta.setLore(ffLore);
        ff.setItemMeta(ffMeta);
        inv.setItem(15, ff);

        // 👥 Miembros (Abajo - Slot 22)
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta memMeta = members.getItemMeta();
        memMeta.setDisplayName("§b§l👥 Lista de Miembros");
        List<String> memLore = new ArrayList<>();
        memLore.add("§7Tu Rango: §b" + user.getClanRole());
        memLore.add("");
        memLore.add("§e▶ (Lista en construcción) 🏗️");
        memMeta.setLore(memLore);
        members.setItemMeta(memMeta);
        inv.setItem(22, members);

        // Abrimos el inventario al jugador
        player.openInventory(inv);
    }
}