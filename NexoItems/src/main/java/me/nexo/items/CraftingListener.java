package me.nexo.items;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos la clase principal de NexoItems
    private final NexoItems plugin;

    public CraftingListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alCraftear(CraftItemEvent event) {
        // Asegurarnos de que fue un jugador el que hizo clic
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player jugador = (Player) event.getWhoClicked();

        // Obtenemos qué es lo que va a salir de la mesa de crafteo
        ItemStack resultado = event.getRecipe().getResult();

        // Si el objeto es normal (madera, palos) y no tiene nombre custom, lo dejamos pasar
        if (!resultado.hasItemMeta() || !resultado.getItemMeta().hasDisplayName()) return;

        // Limpiamos los colores del nombre del ítem para leerlo bien
        String nombreItem = ChatColor.stripColor(resultado.getItemMeta().getDisplayName());

        // ==========================================
        // CANDADOS DE COLECCIONES (CRAFTEOS CUSTOM)
        // ==========================================

        // 1. Candado del Diamante Encantado
        if (nombreItem.contains("Diamante Encantado")) {
            // Verificamos si AuroraCollections ya le dio el permiso secreto
            if (!jugador.hasPermission("nexo.coleccion.diamante1")) {
                event.setCancelled(true); // Bloqueamos el crafteo
                jugador.closeInventory(); // Le cerramos la mesa en la cara
                jugador.sendMessage("§c§l¡RECETA BLOQUEADA! §7Necesitas alcanzar la §bColección de Diamante I §7para craftear esto.");
                jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        // 2. Aquí puedes añadir más candados luego (ej. "Armadura de Esmeralda")
        /*
        else if (nombreItem.contains("Espada del Rey Orco")) {
            if (!jugador.hasPermission("nexo.coleccion.orco3")) {
                event.setCancelled(true);
                jugador.closeInventory();
                jugador.sendMessage("§c§l¡RECETA BLOQUEADA!");
            }
        }
        */
    }
}