package me.nexo.items.mochilas;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    // 🎨 Títulos limpios para validar si el inventario es una Mochila Real
    public static final String TITLE_PREFIX_PLAIN = "» Mochila Virtual #";

    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    // ===============================================
    // 🛡️ LÓGICA DE PROTECCIÓN AL CERRAR Y GUARDADO
    // ===============================================
    @EventHandler
    public void alCerrarMochila(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (tituloLimpio.startsWith(TITLE_PREFIX_PLAIN)) {
            Player p = (Player) event.getPlayer();
            try {
                String idString = tituloLimpio.replace(TITLE_PREFIX_PLAIN, "");
                int id = Integer.parseInt(idString);

                // Guardamos el contenido real en la base de datos o archivo
                manager.guardarMochila(p, id, event.getInventory());
                p.sendMessage(NexoColor.parse("&#55FF55[✓] Sincronización en la nube completada. Mochila #" + id + " asegurada."));

            } catch (NumberFormatException e) {
                p.sendMessage(NexoColor.parse("&#FF5555[!] Error de desincronización al guardar la mochila. Contacta al soporte técnico corporativo."));
            }
        }
    }

    // ===============================================
    // 🛡️ BLOQUEO DE CONTENEDORES ANIDADOS (SHULKERS)
    // ===============================================
    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.startsWith(TITLE_PREFIX_PLAIN)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // 1. Evita mover con Shift+Clic desde el inventario del jugador a la mochila
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.equals(event.getView().getBottomInventory())) {
            if (esMochilaProhibida(event.getCurrentItem())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
            }
        }
        // 2. Evita hacer clic directo con el ítem en el cursor sobre la mochila
        else if (clickedInv.equals(topInv)) {
            if (esMochilaProhibida(event.getCursor())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
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
                        event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
                    }
                }
            }
        }
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.startsWith(TITLE_PREFIX_PLAIN)) return;

        // Evita mantener presionado el clic y arrastrar Shulkers por los slots
        if (esMochilaProhibida(event.getOldCursor())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
                    return;
                }
            }
        }
    }

    private boolean esMochilaProhibida(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String tipo = item.getType().name();

        // Bloqueamos las cajas de Shulker Vanilla (para evitar NBT Dupes o inventarios anidados)
        if (tipo.endsWith("SHULKER_BOX")) return true;

        return false;
    }
}