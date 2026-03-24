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
        // Validación limpia y moderna de Java (Patrón de instancia)
        if (!(event.getWhoClicked() instanceof Player jugador)) return;

        // Obtenemos qué es lo que va a salir de la mesa de crafteo
        ItemStack resultado = event.getRecipe().getResult();

        // Si el objeto es normal (madera, palos) y no tiene nombre custom, lo dejamos pasar
        if (!resultado.hasItemMeta() || !resultado.getItemMeta().hasDisplayName()) return;

        // Limpiamos los colores del nombre del ítem para leerlo bien (Mantenemos ChatColor.stripColor por compatibilidad con ítems legacy)
        String nombreItem = ChatColor.stripColor(resultado.getItemMeta().getDisplayName());

        if (nombreItem == null) return;

        // ==========================================
        // 🔒 CANDADOS DE COLECCIONES (CRAFTEOS CUSTOM)
        // ==========================================

        // 1. Candado del Diamante Encantado
        if (nombreItem.contains("Diamante Encantado")) {
            // Verificamos si AuroraCollections ya le dio el permiso
            if (!jugador.hasPermission("nexo.coleccion.diamante1")) {
                event.setCancelled(true); // Bloqueamos el crafteo
                jugador.closeInventory(); // Le cerramos la mesa en la cara
                jugador.sendMessage(NexoColor.parse("&#FF5555<bold>[!] ENSAMBLAJE DENEGADO</bold> &#555555| &#AAAAAARequieres credenciales de &#00E5FFColección de Diamante I &#AAAAAApara procesar esta matriz."));
                jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        // 2. Aquí puedes añadir más candados luego (ej. "Armadura de Esmeralda")
        /*
        else if (nombreItem.contains("Espada del Rey Orco")) {
            if (!jugador.hasPermission("nexo.coleccion.orco3")) {
                event.setCancelled(true);
                jugador.closeInventory();
                jugador.sendMessage(NexoColor.parse("&#FF5555<bold>[!] ENSAMBLAJE DENEGADO</bold>"));
                jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
        */
    }
}