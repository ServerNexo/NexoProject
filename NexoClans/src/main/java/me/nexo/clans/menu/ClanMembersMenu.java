package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ClanMembersMenu {

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user, NexoClans plugin) {
        player.sendMessage("§eBuscando miembros en los registros del servidor...");

        // Usamos nuestro nuevo método asíncrono
        plugin.getClanManager().getMiembrosAsync(clan.getId(), miembros -> {
            Inventory inv = Bukkit.createInventory(null, 54, "§8§l» §bMiembros del Clan");

            for (int i = 0; i < miembros.size() && i < 54; i++) {
                ClanMember m = miembros.get(i);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();

                // Ponemos la textura de su cabeza real
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));
                meta.setDisplayName("§e" + m.name());

                List<String> lore = new ArrayList<>();
                lore.add("§7Rango: §b" + m.role());
                lore.add("");

                // Si el que abrió el menú es líder/oficial y no se está clickeando a sí mismo
                if ((user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) && !m.uuid().equals(player.getUniqueId())) {
                    lore.add("§c▶ Clic Derecho para Expulsar");
                }

                meta.setLore(lore);
                head.setItemMeta(meta);
                inv.setItem(i, head);
            }

            player.openInventory(inv);
        });
    }
}