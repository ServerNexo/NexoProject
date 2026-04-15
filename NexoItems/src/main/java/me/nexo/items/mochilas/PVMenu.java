package me.nexo.items.mochilas;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Menú de Bóvedas del Vacío / Player Vaults (Arquitectura Enterprise)
 * Nota: Los menús son instanciados por jugador, NO usan @Singleton.
 */
public class PVMenu extends NexoMenu {

    private final NexoItems plugin;

    public PVMenu(Player player, NexoItems plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Título convertido a String seguro para el inventario
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#9933FF🧳 <bold>BÓVEDAS DEL VACÍO</bold>"));
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El cristal morado automático

        for (int i = 1; i <= 9; i++) {
            boolean tienePerm = (i == 1) || player.hasPermission("nexo.pv." + i);

            ItemStack pvItem = new ItemStack(tienePerm ? Material.ENDER_CHEST : Material.MINECART);
            ItemMeta meta = pvItem.getItemMeta();

            if (meta != null) {
                if (tienePerm) {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff✨ <bold>BÓVEDA #" + i + "</bold>"));

                    List<String> loreRaw = List.of(
                            "&#E6CCFFHaz clic para abrir tu",
                            "&#E6CCFFalmacenamiento dimensional.",
                            "",
                            "&#55FF55► Clic para acceder"
                    );
                    meta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

                    // 🌟 PDC para saber a qué mochila le dio clic
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "pv_action"), PersistentDataType.INTEGER, i);
                } else {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF5555🔒 <bold>BÓVEDA #" + i + " BLOQUEADA</bold>"));

                    List<String> loreRaw = List.of(
                            "&#E6CCFFNo tienes acceso a esta",
                            "&#E6CCFFbóveda todavía.",
                            "",
                            "&#FF3366[!] Requiere rango o expansión."
                    );
                    meta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
                }
                pvItem.setItemMeta(meta);
            }

            // Los ponemos en la fila central
            inventory.setItem(i + 8, pvItem);
        }

        // Botón de Volver al Hub
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER AL HUB</bold>"));
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, "open_hub");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo anti-robos en el selector

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey pvKey = new NamespacedKey(plugin, "pv_action");
        NamespacedKey hubKey = new NamespacedKey(plugin, "hub_action");

        // Si dio clic a una Mochila Desbloqueada
        if (meta.getPersistentDataContainer().has(pvKey, PersistentDataType.INTEGER)) {
            int mochilaId = meta.getPersistentDataContainer().get(pvKey, PersistentDataType.INTEGER);

            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
            player.closeInventory();

            // Abre la mochila forzando el comando con retraso anti-bug de Bedrock
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.performCommand("pv " + mochilaId);
            }, 3L);
        }
        // Si dio clic en Volver
        else if (meta.getPersistentDataContainer().has(hubKey, PersistentDataType.STRING)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 🌟 CORRECCIÓN APLICADA: Nuevo orden del constructor (player, plugin) y método .open()
                new me.nexo.core.hub.HubMenu(player, me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)).open();
            }, 3L);
        }
    }
}