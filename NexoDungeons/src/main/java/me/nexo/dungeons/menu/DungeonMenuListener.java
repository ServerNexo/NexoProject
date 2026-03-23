package me.nexo.dungeons.menu;

import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DungeonMenuListener implements Listener {

    private final NexoDungeons plugin;

    public DungeonMenuListener(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(DungeonMenu.MENU_TITLE)) return;
        event.setCancelled(true); // Bloqueamos para que no roben ítems

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11 -> {
                // 🏰 Instancias Privadas
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.sendMessage("§bAbriendo selector de Mazmorras...");
                player.closeInventory();

                // Aquí en el futuro abriremos otro sub-menú (Ej: Criptas, Castillo, etc.)
                // Y llamaríamos a: plugin.getGridManager().pasteDungeonAsync("CriptasT1").thenAccept(loc -> player.teleport(loc));
            }
            case 13 -> {
                // ⚔️ Arenas de Oleadas (Matchmaking)
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
                player.closeInventory();

                // 🌟 NUEVO: Lo metemos al sistema de colas automatizado
                plugin.getQueueManager().addPlayerToWaves(player);
            }
            case 15 -> {
                // 🐉 Bosses Públicos
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                player.sendMessage("§d§lINFO: §7El Altar del Dragón se encuentra en §fX: 0, Z: 0 §7(En el End).");
                player.closeInventory();
            }
        }
    }
}