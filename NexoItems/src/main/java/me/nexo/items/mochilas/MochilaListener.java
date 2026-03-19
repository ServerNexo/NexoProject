package me.nexo.items.mochilas;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MochilaListener implements Listener {

    private final MochilaManager manager;

    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void alCerrarMochila(InventoryCloseEvent event) {
        String titulo = event.getView().getTitle();

        // Detectamos si es nuestro inventario por el título exacto
        if (titulo.startsWith("§8🎒 Mochila Virtual #")) {
            Player p = (Player) event.getPlayer();
            try {
                // Extraemos el número del título (ej: "§8🎒 Mochila Virtual #1" -> "1")
                String idString = titulo.replace("§8🎒 Mochila Virtual #", "");
                int id = Integer.parseInt(idString);

                // Guardamos asíncronamente
                manager.guardarMochila(p, id, event.getInventory());
                p.sendMessage("§a☁ Mochila guardada en la nube.");

            } catch (NumberFormatException e) {
                p.sendMessage("§c[!] Error al guardar la mochila. No muevas ítems importantes y contacta a un administrador.");
            }
        }
    }
}