package me.nexo.protections.menu;

import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class ProtectionMembersMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;

    public ProtectionMembersMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
    }

    // 🌟 MÉTODOS MÁGICOS DE LECTURA
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.miembros.titulo");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        NamespacedKey uuidKey = new NamespacedKey(plugin, "acolyte_uuid");

        int slot = 0;
        for (UUID uuid : stone.getTrustedFriends()) {
            if (slot >= getSlots() - 9) break; // Protegemos los botones de abajo

            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String targetName = target.getName() != null ? target.getName() : "Alma Desconocida";

            // Leemos el nombre y lore desde config
            String headName = getMessage("menus.miembros.items.cabeza.nombre").replace("%player%", targetName);
            List<String> lore = getMessageList("menus.miembros.items.cabeza.lore");

            setItem(slot, Material.PLAYER_HEAD, headName, lore);

            // Recuperamos el ítem para inyectarle la skin y el UUID invisible
            ItemStack head = inventory.getItem(slot);
            if (head != null && head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid.toString());
                head.setItemMeta(meta);
            }
            slot++;
        }

        // Botón Volver
        setItem(getSlots() - 6, Material.ENDER_PEARL, getMessage("menus.miembros.items.volver.nombre"), null);

        // Botón Invocar (Información)
        setItem(getSlots() - 4, Material.WRITABLE_BOOK, getMessage("menus.miembros.items.invocar.nombre"), getMessageList("menus.miembros.items.invocar.lore"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto contra robos
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Clic en Volver
        if (clicked.getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            new ProtectionMenu(player, plugin, stone).open();
            return;
        }

        // Clic en Cabeza (Desterrar)
        NamespacedKey uuidKey = new NamespacedKey(plugin, "acolyte_uuid");
        ItemMeta meta = clicked.getItemMeta();

        if (clicked.getType() == Material.PLAYER_HEAD && meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            // Solo el dueño puede desterrar
            if (!stone.getOwnerId().equals(player.getUniqueId())) return;

            String targetUuidStr = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
            UUID targetUuid = UUID.fromString(targetUuidStr);

            stone.removeFriend(targetUuid);
            plugin.getClaimManager().saveStoneDataAsync(stone);

            player.sendMessage(NexoColor.parse(getMessage("mensajes.exito.destierro")));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

            // Refresca el menú al instante, sin tirones visuales
            inventory.clear();
            setMenuItems();
        }
    }
}