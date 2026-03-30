package me.nexo.protections.menu;

import me.nexo.core.menus.NexoMenu;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ProtectionFlagsMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;

    public ProtectionFlagsMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
    }

    @Override
    public String getMenuName() {
        return "&#1c0f2a<bold>»</bold> &#00f5ffLeyes del Dominio";
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // Fila 1: Entorno General
        createFlagItem(10, Material.NETHERITE_SWORD, "Daño PvP", "pvp");
        createFlagItem(11, Material.ZOMBIE_HEAD, "Aparición de Monstruos", "mob-spawning");
        createFlagItem(12, Material.TNT, "Daño de Explosiones", "tnt-damage");
        createFlagItem(13, Material.FLINT_AND_STEEL, "Propagación de Fuego", "fire-spread");
        createFlagItem(14, Material.LEATHER, "Asesinato de Animales", "animal-damage");

        // Fila 2: Interacciones de Forasteros
        createFlagItem(19, Material.OAK_DOOR, "Uso de Puertas/Botones", "interact");
        createFlagItem(20, Material.CHEST, "Abrir Cofres/Hornos", "containers");
        createFlagItem(21, Material.HOPPER, "Robar Ítems del Suelo", "item-pickup");
        createFlagItem(22, Material.ROTTEN_FLESH, "Tirar Basura (Drop)", "item-drop");
        createFlagItem(23, Material.IRON_DOOR, "Entrada de Forasteros", "ENTRY");

        // Botón Volver
        setItem(getSlots() - 5, Material.ENDER_PEARL, "&#00f5ff<bold>VOLVER AL MONOLITO</bold>", null);
    }

    private void createFlagItem(int slot, Material mat, String nombre, String flagId) {
        boolean activo = stone.getFlag(flagId);
        String estadoColor = activo ? "&#00f5ff[ PERMITIDO ]" : "&#8b0000[ BLOQUEADO ]";

        List<String> lore = List.of(
                "&#1c0f2aPara forasteros: " + estadoColor,
                " ",
                "&#00f5ff► Clic para alternar esta ley"
        );

        setItem(slot, mat, "&#ff00ff<bold>" + nombre.toUpperCase() + "</bold>", lore);

        // Magia PDC: Guardamos la llave interna en el ítem de forma segura
        ItemStack item = inventory.getItem(slot);
        if (item != null && item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey key = new NamespacedKey(plugin, "flag_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, flagId);
            item.setItemMeta(meta);
        }
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

        // Clic en Ley (Flag)
        NamespacedKey key = new NamespacedKey(plugin, "flag_id");
        ItemMeta meta = clicked.getItemMeta();

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            // Solo el dueño puede cambiar flags
            if (!stone.getOwnerId().equals(player.getUniqueId())) return;

            String flagId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            boolean actual = stone.getFlag(flagId);

            stone.setFlag(flagId, !actual); // Invierte la ley
            plugin.getClaimManager().saveStoneDataAsync(stone);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);

            // 🌟 MAGIA: Refresca el menú al instante, cambiando colores sin cerrar la ventana
            setMenuItems();
        }
    }
}