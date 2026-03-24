package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ClanMembersMenu {

    // 🎨 CONSTANTES
    public static final String TITLE_MEMBERS = "&#434343<bold>»</bold> &#00fbffRegistro de Operarios";
    private static final String MSG_SEARCHING = "&#fbd72bConectando con la base de datos de Nexo...";

    public static void abrirMenu(Player player, NexoClan clan, NexoUser user, NexoClans plugin) {
        player.sendMessage(NexoColor.parse(MSG_SEARCHING));

        // Hilo asíncrono
        plugin.getClanManager().getMiembrosAsync(clan.getId(), miembros -> {
            Inventory inv = Bukkit.createInventory(null, 54, NexoColor.parse(TITLE_MEMBERS));

            for (int i = 0; i < miembros.size() && i < 54; i++) {
                ClanMember m = miembros.get(i);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();

                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));
                    meta.displayName(NexoColor.parse("&#fbd72b" + m.name()));

                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(NexoColor.parse("&#434343Autorización: &#00fbff" + m.role()));
                    lore.add(NexoColor.parse(" "));

                    // Lógica de expulsión
                    if ((user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) && !m.uuid().equals(player.getUniqueId())) {
                        lore.add(NexoColor.parse("&#ff4b2b▶ Clic Derecho para Revocar Acceso (Expulsar)"));
                    }

                    meta.lore(lore);
                    head.setItemMeta(meta);
                }
                inv.setItem(i, head);
            }

            // Sincronizamos con el hilo principal para abrir el inventario
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }
}