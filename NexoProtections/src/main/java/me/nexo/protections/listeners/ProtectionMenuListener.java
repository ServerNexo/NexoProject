package me.nexo.protections.listeners;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ProtectionMenuListener implements Listener {

    private final ClaimManager claimManager;

    public ProtectionMenuListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String tituloMenu = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (tituloMenu.contains("Monolito del Vacío")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            // Botón: Miembros (Acólitos)
            if (event.getSlot() == 11) {
                player.closeInventory();
                player.sendMessage(NexoColor.parse(" "));
                player.sendMessage(NexoColor.parse("&#9933FF<bold>PACTOS DE SANGRE:</bold>"));
                player.sendMessage(NexoColor.parse("&#E6CCFFPara invocar a un alma a tus tierras: &#CC66FF/nexo trust <Amigo>"));
                player.sendMessage(NexoColor.parse("&#E6CCFFPara desterrar a un acólito: &#FF3366/nexo untrust <Amigo>"));
                player.sendMessage(NexoColor.parse(" "));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
            // Botón: Leyes del Dominio (Flags)
            else if (event.getSlot() == 15) {
                if (!stone.getOwnerId().equals(player.getUniqueId())) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Solo el Señor del Dominio puede alterar las leyes naturales."));
                    return;
                }
                me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone);
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
            }
            // Botón: Infundir Esencia (Recarga con Diamantes o Fragmentos)
            else if (event.getSlot() == 22) {
                if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] El Monolito está completamente saciado."));
                    return;
                }

                // Prioriza Fragmentos de Eco, luego Diamantes
                if (player.getInventory().contains(Material.ECHO_SHARD)) {
                    quitarItem(player, Material.ECHO_SHARD, 1);
                    stone.addEnergy(500); // Rinde mucho
                    recargaExitosa(player);
                } else if (player.getInventory().contains(Material.DIAMOND)) {
                    quitarItem(player, Material.DIAMOND, 1);
                    stone.addEnergy(100);
                    recargaExitosa(player);
                } else {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Ofrenda Rechazada: &#E6CCFFNecesitas Diamantes o Fragmentos de Eco para alimentar el vacío."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }

        // LÓGICA DEL MENÚ DE LEYES (FLAGS)
        if (tituloMenu.contains("Leyes del Dominio")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            if (event.getSlot() == 22) { // Botón Volver
                me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                return;
            }

            String flagId = null;
            if (event.getSlot() == 10) flagId = "pvp";
            if (event.getSlot() == 12) flagId = "mob-spawning";
            if (event.getSlot() == 14) flagId = "tnt-damage";
            if (event.getSlot() == 16) flagId = "fire-spread";

            if (flagId != null) {
                boolean actual = stone.getFlag(flagId);
                stone.setFlag(flagId, !actual);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);

                // Refrescar menú
                me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone);
            }
        }
    }

    private void recargaExitosa(Player player) {
        player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>ESENCIA DEVORADA:</bold> &#E6CCFFEl Monolito ha absorbido tu ofrenda."));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f);
        player.closeInventory();
    }

    private void quitarItem(Player player, Material mat, int cantidad) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                if (item.getAmount() > cantidad) {
                    item.setAmount(item.getAmount() - cantidad);
                    break;
                } else {
                    cantidad -= item.getAmount();
                    player.getInventory().remove(item);
                    if (cantidad <= 0) break;
                }
            }
        }
    }
}