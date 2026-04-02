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
import java.util.stream.Collectors;

public class ProtectionFlagsMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;

    public ProtectionFlagsMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
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
        return getMessage("menus.leyes.titulo");
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // Fila 1: Entorno General
        createFlagItem(10, Material.NETHERITE_SWORD, getMessage("menus.leyes.flags.pvp"), "pvp");
        createFlagItem(11, Material.ZOMBIE_HEAD, getMessage("menus.leyes.flags.mob-spawning"), "mob-spawning");
        createFlagItem(12, Material.TNT, getMessage("menus.leyes.flags.tnt-damage"), "tnt-damage");
        createFlagItem(13, Material.FLINT_AND_STEEL, getMessage("menus.leyes.flags.fire-spread"), "fire-spread");
        createFlagItem(14, Material.LEATHER, getMessage("menus.leyes.flags.animal-damage"), "animal-damage");

        // Fila 2: Interacciones de Forasteros
        createFlagItem(19, Material.OAK_DOOR, getMessage("menus.leyes.flags.interact"), "interact");
        createFlagItem(20, Material.CHEST, getMessage("menus.leyes.flags.containers"), "containers");
        createFlagItem(21, Material.HOPPER, getMessage("menus.leyes.flags.item-pickup"), "item-pickup");
        createFlagItem(22, Material.ROTTEN_FLESH, getMessage("menus.leyes.flags.item-drop"), "item-drop");
        createFlagItem(23, Material.IRON_DOOR, getMessage("menus.leyes.flags.ENTRY"), "ENTRY");

        // Botón Volver
        setItem(getSlots() - 5, Material.ENDER_PEARL, getMessage("menus.leyes.items.volver.nombre"), null);
    }

    private void createFlagItem(int slot, Material mat, String nombre, String flagId) {
        boolean activo = stone.getFlag(flagId);
        String estadoColor = activo ? getMessage("menus.leyes.items.flag.estado-permitido") : getMessage("menus.leyes.items.flag.estado-bloqueado");

        // Formateamos el lore dinámicamente
        List<String> lore = getMessageList("menus.leyes.items.flag.lore").stream()
                .map(line -> line.replace("%status%", estadoColor))
                .collect(Collectors.toList());

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