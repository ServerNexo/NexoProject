package me.nexo.items.mecanicas;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlayerItemListener implements Listener {

    private final NexoItems plugin;

    public PlayerItemListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();

        if (!jugador.hasPlayedBefore()) {
            jugador.getInventory().addItem(ItemManager.generarArmaRPG("baculo_manantial_t1"));
            CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.items.bienvenida"));
        }
    }

    @EventHandler
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(event.getPlayer(), plugin.getConfigManager().getMessage("eventos.items.restringido"));
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}