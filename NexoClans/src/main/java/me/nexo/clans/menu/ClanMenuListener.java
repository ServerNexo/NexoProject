package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
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
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null || !user.hasClan()) return;

        // 🌟 VALIDACIÓN DINÁMICA DE TÍTULOS (Soporte Kyori + Crossplay)
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String expectedMainMenu = PlainTextComponentSerializer.plainText().serialize(
                CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.principal.titulo"))
        );

        String expectedMembersMenu = PlainTextComponentSerializer.plainText().serialize(
                CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.miembros.titulo"))
        );

        // ==========================================
        // 🏰 MENÚ PRINCIPAL
        // ==========================================
        if (plainTitle.equals(expectedMainMenu)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            // 🌟 CORRECCIÓN DE SLOTS: Clic en Fuego Amigo (Slot 22 según tu ClanMenu.java)
            if (event.getRawSlot() == 22) {
                if (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL")) {
                    plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                        plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire());
                        player.closeInventory();
                    });
                }
            }

            // 🌟 CORRECCIÓN DE SLOTS: Clic en Miembros (Slot 24 según tu ClanMenu.java)
            if (event.getRawSlot() == 24) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    ClanMembersMenu.abrirMenu(player, clan, user, plugin);
                });
            }
            return;
        }

        // ==========================================
        // 👥 SUB-MENÚ DE MIEMBROS
        // ==========================================
        if (plainTitle.equals(expectedMembersMenu)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            // Botón de Regresar (Slot 49 en ClanMembersMenu.java)
            if (event.getCurrentItem().getType() == Material.ARROW && event.getRawSlot() == 49) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    ClanMenu.abrirMenu(player, clan, user, plugin);
                });
                return;
            }

            if (event.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

            if (event.getClick().isRightClick() && event.getCurrentItem().getItemMeta() != null) {
                // Extraemos el nombre limpio (sin códigos Hex)
                String targetName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());

                // Ejecución invisible del comando de expulsión
                player.performCommand("clan kick " + targetName);
                player.closeInventory();
            }
        }
    }
}