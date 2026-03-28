package me.nexo.items.mecanicas;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
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
            jugador.sendMessage(NexoColor.parse("&#ff00ff<bold>NEXO</bold> &#1c0f2a| &#1c0f2a¡Bienvenido al sistema! Tu primer activo de combate ha sido asignado a tu inventario."));
        }
    }

    @EventHandler
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(NexoColor.parse("&#8b0000<bold>[🔒] RESTRICCIÓN BIOMÉTRICA:</bold> &#1c0f2aNo puedes descartar un objeto enlazado permanentemente a tu firma neuronal."));
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}