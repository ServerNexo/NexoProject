package me.nexo.items.mochilas;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    // 🎨 Títulos y prefijos limpios para validaciones
    public static final String TITLE_PREFIX_PLAIN = "» Mochila Virtual #";

    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void alCerrarMochila(InventoryCloseEvent event) {
        // 🌟 CORRECCIÓN 1.21: Usamos PlainTextComponentSerializer en vez de getTitle()
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (tituloLimpio.startsWith(TITLE_PREFIX_PLAIN)) {
            Player p = (Player) event.getPlayer();
            try {
                String idString = tituloLimpio.replace(TITLE_PREFIX_PLAIN, "");
                int id = Integer.parseInt(idString);

                manager.guardarMochila(p, id, event.getInventory());
                p.sendMessage(NexoColor.parse("&#55FF55[✓] Sincronización en la nube completada. Mochila #" + id + " asegurada."));

            } catch (NumberFormatException e) {
                p.sendMessage(NexoColor.parse("&#FF5555[!] Error de desincronización al guardar la mochila. Contacta al soporte técnico corporativo."));
            }
        }
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.startsWith(TITLE_PREFIX_PLAIN)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.equals(event.getView().getBottomInventory())) {
            if (esMochilaProhibida(event.getCurrentItem())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
            }
        }
        else if (clickedInv.equals(topInv)) {
            // 🌟 CORRECCIÓN 1.21: Usamos event.getCursor() de manera segura o event.getWhoClicked().getItemOnCursor()
            if (esMochilaProhibida(event.getCursor())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(NexoColor.parse("&#FF5555[!] Infracción detectada: Prohibido almacenar contenedores anidados en la mochila virtual."));
            }
        }
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

        // Bloqueamos las cajas de Shulker Vanilla
        if (tipo.endsWith("SHULKER_BOX")) return true;

        return false;
    }
}