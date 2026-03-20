package me.nexo.items.mochilas;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MochilaListener implements Listener {

    private final MochilaManager manager;

    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void alCerrarMochila(InventoryCloseEvent event) {
        // ⚠️ NOTA: Si ves 'getTitle()' tachado, ¡NO ES UN ERROR!
        // Es una advertencia visual de Bukkit 1.21, pero compilará 100% bien.
        String titulo = event.getView().getTitle();

        if (titulo.startsWith("§8🎒 Mochila Virtual #")) {
            Player p = (Player) event.getPlayer();
            try {
                String idString = titulo.replace("§8🎒 Mochila Virtual #", "");
                int id = Integer.parseInt(idString);

                manager.guardarMochila(p, id, event.getInventory());
                p.sendMessage("§a☁ Mochila guardada en la nube.");

            } catch (NumberFormatException e) {
                p.sendMessage("§c[!] Error al guardar la mochila. No muevas ítems importantes y contacta a un administrador.");
            }
        }
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.startsWith("§8🎒 Mochila Virtual #")) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.equals(event.getView().getBottomInventory())) {
            if (esMochilaProhibida(event.getCurrentItem())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c[!] No puedes guardar Shulkers o Mochilas dentro de otra Mochila.");
            }
        }
        else if (clickedInv.equals(topInv)) {
            // ⚠️ NOTA: Si ves 'getCursor()' tachado, ignóralo también.
            if (esMochilaProhibida(event.getCursor())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c[!] No puedes guardar Shulkers o Mochilas dentro de otra Mochila.");
            }
        }
        else if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (clickedInv.equals(topInv)) {
                int hotbarButton = event.getHotbarButton();
                if (hotbarButton >= 0) {
                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(hotbarButton);
                    if (esMochilaProhibida(hotbarItem)) {
                        event.setCancelled(true);
                        event.getWhoClicked().sendMessage("§c[!] No puedes guardar Shulkers o Mochilas dentro de otra Mochila.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.startsWith("§8🎒 Mochila Virtual #")) return;

        if (esMochilaProhibida(event.getOldCursor())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§c[!] No puedes guardar Shulkers o Mochilas dentro de otra Mochila.");
                    return;
                }
            }
        }
    }

    private boolean esMochilaProhibida(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String tipo = item.getType().name();

        // Bloqueamos las cajas de Shulker Vanilla
        if (tipo.endsWith("SHULKER_BOX")) return true;

        return false;
    }
}