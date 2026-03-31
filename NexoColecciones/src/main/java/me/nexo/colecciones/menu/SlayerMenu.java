package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class SlayerMenu extends NexoMenu {

    private final NexoColecciones plugin;

    public SlayerMenu(Player player, NexoColecciones plugin) {
        super(player);
        this.plugin = plugin;
    }

    // 🌟 CORRECCIÓN 1: Usamos el ConfigManager local e independiente
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.slayer.titulo");
    }

    @Override
    public int getSlots() {
        return 27; // Inventario de 3 filas
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena los huecos vacíos con cristal morado del Vacío

        SlayerManager manager = plugin.getSlayerManager();
        List<String> loreConfig = getMessageList("menus.slayer.item-lore");

        int slot = 10; // Empezamos a colocar los jefes en el centro de la interfaz

        // 🌟 CORRECCIÓN 2: Leemos correctamente desde tu SlayerTemplate
        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {
            if (slot >= 17) break; // Límite de seguridad visual (1 fila central)

            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name() + "</bold>"));

                // 🌟 CORRECCIÓN 3: Reemplazamos variables y forzamos el color Lila Iluminado
                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line
                                .replace("%id%", template.id())
                                .replace("%kills%", String.valueOf(template.requiredKills()))
                                .replace("%mob%", template.targetMob())
                                .replace("%boss_name%", template.bossName())
                                .replace("&#1c0f2a", "&#E6CCFF"))) // Sustitución directa de color
                        .collect(Collectors.toList());

                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                // 🌟 CORRECCIÓN 4: Agregamos PersistentDataContainer para procesar el clic
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "start_slayer");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "slayer_id"), PersistentDataType.STRING, template.id());

                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        // 🌟 CORRECCIÓN 5: Ejecutamos el contrato de caza usando tu método real "iniciarSlayer"
        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action.equals("start_slayer")) {
                String slayerId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "slayer_id"), PersistentDataType.STRING);

                player.closeInventory();

                // Iniciamos la cacería
                plugin.getSlayerManager().iniciarSlayer(player, slayerId);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
        }
    }
}