package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Singleton
public class MochilaListener implements Listener {

    private final MochilaManager manager;

    @Inject
    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    // ===============================================
    // 🛡️ LÓGICA DE PROTECCIÓN AL CERRAR Y GUARDADO
    // ===============================================
    @EventHandler(priority = EventPriority.HIGH)
    public void alCerrarMochila(InventoryCloseEvent event) {
        // 🌟 FIX: Adiós títulos. Hola InventoryHolder seguro.
        if (event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder holder) {
            Player p = (Player) event.getPlayer();
            int id = holder.getMochilaId();

            manager.guardarMochila(p, id, event.getInventory());
            CrossplayUtils.sendMessage(p, "&#55FF55[✓] Sincronización en la nube completada. Bóveda #" + id + " asegurada.");
        }
    }

    // ===============================================
    // 🛡️ BLOQUEO DE CONTENEDORES ANIDADOS (SHULKERS)
    // ===============================================
    @EventHandler(priority = EventPriority.HIGH)
    public void alHacerClic(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // 1. Evita mover con Shift+Clic desde el inventario del jugador a la mochila
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.equals(event.getView().getBottomInventory())) {
            if (esMochilaProhibida(event.getCurrentItem())) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage((Player) event.getWhoClicked(), "&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual.");
            }
        }
        // 2. Evita hacer clic directo con el ítem en el cursor sobre la mochila
        else if (clickedInv.equals(topInv)) {
            if (esMochilaProhibida(event.getCursor())) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage((Player) event.getWhoClicked(), "&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual.");
            }
        }
        // 3. Evita usar los números de la Hotbar (1-9) para intercambiar ítems a la mochila
        else if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (clickedInv.equals(topInv)) {
                int hotbarButton = event.getHotbarButton();
                if (hotbarButton >= 0) {
                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(hotbarButton);
                    if (esMochilaProhibida(hotbarItem)) {
                        event.setCancelled(true);
                        CrossplayUtils.sendMessage((Player) event.getWhoClicked(), "&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alArrastrar(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder)) return;

        // Evita mantener presionado el clic y arrastrar Shulkers por los slots
        if (esMochilaProhibida(event.getOldCursor())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    CrossplayUtils.sendMessage((Player) event.getWhoClicked(), "&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual.");
                    return;
                }
            }
        }
    }

    private boolean esMochilaProhibida(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String tipo = item.getType().name();
        // Bloqueamos las cajas de Shulker Vanilla (para evitar NBT Dupes o inventarios anidados)
        return tipo.endsWith("SHULKER_BOX");
    }
}