package me.nexo.protections.listeners;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
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
        if (event.getView().title() == null) return;

        // 🌟 Ignorar acentos y mayúsculas para evitar errores de lectura cruzada en Bedrock
        String tituloOriginal = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String tituloMenu = tituloOriginal.toLowerCase();

        if (event.getCurrentItem() == null) return;
        Player player = (Player) event.getWhoClicked();

        // ==========================================
        // 1. MENÚ PRINCIPAL
        // ==========================================
        if (tituloMenu.contains("monolito")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            if (event.getRawSlot() == 11) { // Acólitos
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                // 🌟 BEDROCK FIX: Cerrar inventario actual y esperar 3 ticks
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionMembersMenu.openMenu(player, stone);
                }, 3L);
            }
            else if (event.getRawSlot() == 15) { // Flags
                if (!stone.getOwnerId().equals(player.getUniqueId())) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Solo el Señor del Dominio puede alterar las leyes naturales."));
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);

                // 🌟 BEDROCK FIX: Cerrar inventario actual y esperar 3 ticks
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone);
                }, 3L);
            }
            else if (event.getRawSlot() == 22) { // Recarga
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
        else if (tituloMenu.contains("leyes")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            // 🌟 BEDROCK FIX: Detectar la perla en lugar de un número de slot
            if (event.getCurrentItem().getType() == Material.ENDER_PEARL) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                }, 3L);
                return;
            }

            NamespacedKey key = new NamespacedKey(NexoProtections.getInstance(), "flag_id");
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String flagId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                boolean actual = stone.getFlag(flagId);
                stone.setFlag(flagId, !actual); // Invierte la ley

                claimManager.saveStoneDataAsync(stone);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);

                // 🌟 BEDROCK FIX: Cerrar inventario actual y esperar 3 ticks
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionFlagsMenu.openMenu(player, stone);
                }, 3L);
            }
        }

        // ==========================================
        // 3. MENÚ DE ACÓLITOS
        // ==========================================
        else if (tituloMenu.contains("acólitos") || tituloMenu.contains("acolitos")) {
            event.setCancelled(true);
            ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
            if (stone == null) { player.closeInventory(); return; }

            // 🌟 BEDROCK FIX: Detectar la perla en lugar de un número de slot
            if (event.getCurrentItem().getType() == Material.ENDER_PEARL) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                }, 3L);
                return;
            }

            // Detectar clic en una Cabeza (Desterrar)
            NamespacedKey uuidKey = new NamespacedKey(NexoProtections.getInstance(), "acolyte_uuid");
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {

                if (!stone.getOwnerId().equals(player.getUniqueId())) return;

                String targetUuidStr = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                UUID targetUuid = UUID.fromString(targetUuidStr);

                stone.removeFriend(targetUuid);
                claimManager.saveStoneDataAsync(stone);

                player.sendMessage(NexoColor.parse("&#FF3366[!] DESTIERRO: &#E6CCFFEl alma ha sido expulsada de tu Monolito."));
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

                // 🌟 BEDROCK FIX: Cerrar inventario actual y esperar 3 ticks
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(NexoProtections.getInstance(), () -> {
                    me.nexo.protections.menu.ProtectionMembersMenu.openMenu(player, stone);
                }, 3L);
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