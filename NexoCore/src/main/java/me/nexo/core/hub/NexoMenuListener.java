package me.nexo.core.hub;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class NexoMenuListener implements Listener {

    private final NamespacedKey hubKey;

    public NexoMenuListener(NexoCore plugin) {
        this.hubKey = new NamespacedKey(plugin, "hub_star");
    }

    private void giveHubItem(Player p) {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        if (meta != null) {
            // 🌟 Serializador seguro para evitar errores "null components"
            String safeName = LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("<gradient:#fbd72b:#ff4b2b><bold>Terminal Nexo</bold></gradient>"));
            meta.setDisplayName(safeName);

            // Marca de agua criptográfica
            meta.getPersistentDataContainer().set(hubKey, PersistentDataType.BYTE, (byte) 1);
            star.setItemMeta(meta);
        }
        // Slot 8 = El noveno espacio en la hotbar del jugador
        p.getInventory().setItem(8, star);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        giveHubItem(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        giveHubItem(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked != null && clicked.hasItemMeta()) {
            if (clicked.getItemMeta().getPersistentDataContainer().has(hubKey, PersistentDataType.BYTE)) {
                e.setCancelled(true);
                // Apertura futura del Menú Global
                // e.getWhoClicked().sendMessage(NexoColor.parse("&#00fbff[NEXO] Conectando a la red global..."));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        ItemStack dragged = e.getOldCursor();
        if (dragged.hasItemMeta() && dragged.getItemMeta().getPersistentDataContainer().has(hubKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (dropped.hasItemMeta() && dropped.getItemMeta().getPersistentDataContainer().has(hubKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }
}