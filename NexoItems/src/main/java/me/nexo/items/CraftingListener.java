package me.nexo.items;

import me.nexo.core.utils.NexoColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    private final NexoItems plugin;

    public CraftingListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alCraftear(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) return;

        ItemStack resultado = event.getRecipe().getResult();

        if (!resultado.hasItemMeta() || !resultado.getItemMeta().hasDisplayName()) return;

        String nombreItem = ChatColor.stripColor(resultado.getItemMeta().getDisplayName());

        if (nombreItem == null) return;

        if (nombreItem.contains("Diamante Encantado")) {
            if (!jugador.hasPermission("nexo.coleccion.diamante1")) {
                event.setCancelled(true);
                jugador.closeInventory();
                jugador.sendMessage(NexoColor.parse("&#8b0000<bold>[!] ENSAMBLAJE DENEGADO</bold> &#1c0f2a| &#1c0f2aRequieres credenciales de &#00f5ffColección de Diamante I &#1c0f2apara procesar esta matriz."));
                jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
}