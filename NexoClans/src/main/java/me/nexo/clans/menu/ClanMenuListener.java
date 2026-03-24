package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

        // 🌟 COMPARACIÓN SEGURA DE COMPONENTES DE PAPER
        net.kyori.adventure.text.Component titleComp = event.getView().title();

        // MENÚ PRINCIPAL
        if (titleComp.equals(NexoColor.parse(ClanMenu.TITLE_MENU))) {
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
            return;
        }

        // SUB-MENÚ DE MIEMBROS
        if (titleComp.equals(NexoColor.parse(ClanMembersMenu.TITLE_MEMBERS))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

            if (event.getClick().isRightClick() && event.getCurrentItem().getItemMeta() != null) {
                // Extraemos el nombre sin colores HEX usando el serializador nativo de Paper
                String targetName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());

                // Ejecución invisible del comando de expulsión
                player.performCommand("clan kick " + targetName);
                player.closeInventory();
            }
        }
    }
}