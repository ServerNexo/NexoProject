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

        // 🎁 Entrega del arma inicial si es su primera vez
        if (!jugador.hasPlayedBefore()) {
            jugador.getInventory().addItem(ItemManager.generarArmaRPG("baculo_manantial_t1"));
            jugador.sendMessage(NexoColor.parse("&#FFAA00<bold>NEXO</bold> &#555555| &#AAAAAA¡Bienvenido al sistema! Tu primer activo de combate ha sido asignado a tu inventario."));
        }
    }

    // ==========================================
    // 🔒 SISTEMA ANTI-DROP (SOULBOUND)
    // ==========================================
    @EventHandler
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // Revisamos si el ítem tiene la llave invisible "Soulbound"
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true); // Cancelamos que lo tire
            event.getPlayer().sendMessage(NexoColor.parse("&#FF5555<bold>[🔒] RESTRICCIÓN BIOMÉTRICA:</bold> &#AAAAAANo puedes descartar un objeto enlazado permanentemente a tu firma neuronal."));
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}