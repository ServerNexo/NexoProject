package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
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

    public static final String TITLE_MEMBERS = "&#1c0f2a<bold>»</bold> &#00f5ffRegistro de Operarios";
    private static final String MSG_SEARCHING = "&#00f5ffConectando con la base de datos de Nexo...";

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user, NexoClans plugin) {
        CrossplayUtils.sendMessage(player, MSG_SEARCHING);

        plugin.getClanManager().getMiembrosAsync(clan.getId(), miembros -> {
            Inventory inv = Bukkit.createInventory(null, 54, CrossplayUtils.parseCrossplay(player, TITLE_MEMBERS));

            for (int i = 0; i < miembros.size() && i < 54; i++) {
                ClanMember m = miembros.get(i);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();

                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff" + m.name()));
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aRango: &#00f5ff" + m.role()));
                    meta.lore(lore);
                    head.setItemMeta(meta);
                }
                inv.addItem(head);
            }

            ItemStack back = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta backMeta = back.getItemMeta();
            if (backMeta != null) {
                backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ffRegresar al Monolito Central"));
                back.setItemMeta(backMeta);
            }
            inv.setItem(49, back);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }
}