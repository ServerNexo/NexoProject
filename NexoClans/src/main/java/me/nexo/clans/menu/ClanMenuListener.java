package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClanMenuListener implements Listener {

    private final NexoClans plugin;

    public ClanMenuListener(NexoClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null || !user.hasClan()) return;

        // 🌟 SI ESTAMOS EN EL MENÚ PRINCIPAL
        if (event.getView().getTitle().equals("§8§l» §eTu Clan")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            // Clic en Fuego Amigo (Slot 15)
            if (event.getRawSlot() == 15) {
                if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                    plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                        plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire());
                        player.closeInventory();
                    });
                }
            }

            // Clic en Miembros (Slot 22)
            if (event.getRawSlot() == 22) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    ClanMembersMenu.abrirMenu(player, clan, user, plugin);
                });
            }
        }

        // 🌟 SI ESTAMOS EN EL SUB-MENÚ DE MIEMBROS
        if (event.getView().getTitle().equals("§8§l» §bMiembros del Clan")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

            // Si da clic derecho para expulsar
            if (event.getClick().isRightClick()) {
                String targetName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

                // Obligamos al jugador a ejecutar el comando invisiblemente (Reutilizamos la lógica blindada que ya creaste)
                player.performCommand("clan kick " + targetName);
                player.closeInventory();
            }
        }
    }
}