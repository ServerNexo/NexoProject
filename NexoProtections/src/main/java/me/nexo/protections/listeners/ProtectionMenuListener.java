package me.nexo.protections.listeners;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class ProtectionMenuListener implements Listener {

    private final ClaimManager claimManager;

    public ProtectionMenuListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String tituloMenu = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getCurrentItem() == null) return;
        Player player = (Player) event.getWhoClicked();

        // ==========================================
        // 1. MENÚ PRINCIPAL
        // ==========================================
        if (tituloMenu.contains("Monolito del Vacío")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            if (event.getSlot() == 11) { // Acólitos
                me.nexo.protections.menu.ProtectionMembersMenu.openMenu(player, stone);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
            else if (event.getSlot() == 15) { // Flags
                if (!stone.getOwnerId().equals(player.getUniqueId())) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Solo el Señor del Dominio puede alterar las leyes naturales."));
                    return;
                }
                me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone);
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
            }
            else if (event.getSlot() == 22) { // Recarga
                if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] El Monolito está completamente saciado."));
                    return;
                }
                if (player.getInventory().contains(Material.ECHO_SHARD)) {
                    quitarItem(player, Material.ECHO_SHARD, 1);
                    stone.addEnergy(500);
                    recargaExitosa(player);
                } else if (player.getInventory().contains(Material.DIAMOND)) {
                    quitarItem(player, Material.DIAMOND, 1);
                    stone.addEnergy(100);
                    recargaExitosa(player);
                } else {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Ofrenda Rechazada: &#E6CCFFNecesitas Diamantes o Fragmentos de Eco para alimentar el vacío."));
                }
            }
        }

        // ==========================================
        // 2. MENÚ DE LEYES (FLAGS)
        // ==========================================
        else if (tituloMenu.contains("Leyes del Dominio")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            if (event.getSlot() == 40) { // Volver
                me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                return;
            }

            NamespacedKey key = new NamespacedKey(NexoProtections.getInstance(), "flag_id");
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String flagId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                boolean actual = stone.getFlag(flagId);
                stone.setFlag(flagId, !actual); // Invierte la ley

                // 🌟 GUARDADO ASÍNCRONO EN SUPABASE
                claimManager.saveStoneDataAsync(stone);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);
                me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone); // Refresca
            }
        }

        // ==========================================
        // 3. MENÚ DE ACÓLITOS
        // ==========================================
        else if (tituloMenu.contains("Acólitos del Pacto")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            if (event.getSlot() == 48) { // Volver
                me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                return;
            }

            // Detectar clic en una Cabeza (Desterrar)
            NamespacedKey uuidKey = new NamespacedKey(NexoProtections.getInstance(), "acolyte_uuid");
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {

                // Si alguien más está viendo el menú, no le dejamos borrar
                if (!stone.getOwnerId().equals(player.getUniqueId())) return;

                String targetUuidStr = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                UUID targetUuid = UUID.fromString(targetUuidStr);

                stone.removeFriend(targetUuid); // Lo desterramos de la RAM

                // 🌟 GUARDADO ASÍNCRONO EN SUPABASE
                claimManager.saveStoneDataAsync(stone);

                player.sendMessage(NexoColor.parse("&#FF3366[!] DESTIERRO: &#E6CCFFEl alma ha sido expulsada de tu Monolito."));
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
                me.nexo.protections.menu.ProtectionMembersMenu.openMenu(player, stone); // Refresca
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