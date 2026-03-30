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

    @Override
    public String getMenuName() {
        return "&#1c0f2a<bold>»</bold> &#00f5ffAcólitos del Pacto";
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

            List<String> lore = List.of(
                    "&#1c0f2aEsta alma tiene libre albedrío",
                    "&#1c0f2adentro de tu Monolito.",
                    " ",
                    "&#8b0000► Clic para DESTERRAR esta alma"
            );

            // Usamos el método setItem del NexoMenu base
            setItem(slot, Material.PLAYER_HEAD, "&#ff00ff<bold>" + targetName + "</bold>", lore);

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
        setItem(getSlots() - 6, Material.ENDER_PEARL, "&#00f5ff<bold>VOLVER AL MONOLITO</bold>", null);

        // Botón Invocar (Información)
        List<String> addLore = List.of(
                "&#1c0f2aPara añadir a un amigo,",
                "&#1c0f2acierra este menú y escribe en el chat:",
                "&#ff00ff/nexo trust <NombreJugador>"
        );
        setItem(getSlots() - 4, Material.WRITABLE_BOOK, "&#ff00ff<bold>INVOCAR NUEVA ALMA</bold>", addLore);
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

            player.sendMessage(NexoColor.parse("&#FF3366[!] DESTIERRO: &#E6CCFFEl alma ha sido expulsada de tu Monolito."));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

            // Refresca el menú al instante, sin tirones visuales
            inventory.clear();
            setMenuItems();
        }
    }
}