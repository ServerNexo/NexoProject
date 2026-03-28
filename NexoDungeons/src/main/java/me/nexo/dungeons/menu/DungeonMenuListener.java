package me.nexo.dungeons.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(plugin.getConfigManager().getMessage("menus.principal.titulo").replaceAll("<[^>]*>", ""))) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("menus.listener.conectando-fortalezas"));
                player.closeInventory();
            }
            case 13 -> {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
                player.closeInventory();
                plugin.getQueueManager().addPlayerToWaves(player);
            }
            case 15 -> {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("menus.listener.info-altar"));
                player.closeInventory();
            }
        }
    }
}