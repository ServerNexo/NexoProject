package me.nexo.items.mochilas;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MochilaListener implements Listener {

    private final MochilaManager manager;
    private final NexoItems plugin; // 🌟 Añadido para el Selector de Mochilas

    // 🎨 Títulos y prefijos limpios para validaciones
    public static final String TITLE_PREFIX_PLAIN = "» Mochila Virtual #";

    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
        this.plugin = NexoItems.getPlugin(NexoItems.class); // Obtenemos el plugin de forma segura
    }

    // ===============================================
    // 🎒 LECTURA DE BOTONES PARA EL SELECTOR (PVMenu)
    // ===============================================
    @EventHandler
    public void onPVMenuClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof PVMenu) {
            event.setCancelled(true); // Protege la interfaz para que no roben ítems
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            NamespacedKey pvKey = new NamespacedKey(plugin, "pv_action");
            NamespacedKey hubKey = new NamespacedKey(plugin, "hub_action");

            // Si le dio clic a una de las Mochilas
            if (meta.getPersistentDataContainer().has(pvKey, PersistentDataType.INTEGER)) {
                int mochilaId = meta.getPersistentDataContainer().get(pvKey, PersistentDataType.INTEGER);

                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
                player.closeInventory();

                // Abre la mochila real forzando el comando con 3 ticks de retraso para Bedrock
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.performCommand("pv " + mochilaId);
                }, 3L);
            }
            // Si le dio clic al botón de "Volver al Grimorio"
            else if (meta.getPersistentDataContainer().has(hubKey, PersistentDataType.STRING)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                player.closeInventory();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new me.nexo.core.hub.HubMenu(me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class), player).openMenu();
                }, 3L);
            }
        }
    }

    // ===============================================
    // 🛡️ LÓGICA DE PROTECCIÓN AL CERRAR Y MOVER ÍTEMS
    // ===============================================
    @EventHandler
    public void alCerrarMochila(InventoryCloseEvent event) {
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