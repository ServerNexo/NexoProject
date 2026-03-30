package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class SlayerMenu extends NexoMenu {

    private final NexoColecciones plugin;
    private final NexoCore core;

    public SlayerMenu(Player player, NexoColecciones plugin) {
        super(player);
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @Override
    public String getMenuName() {
        return core.getConfigManager().getMessage("colecciones_messages.yml", "menus.slayer.titulo");
    }

    @Override
    public int getSlots() {
        return 27; // Inventario de 3 filas
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena los huecos vacíos con cristal morado del Vacío

        SlayerManager manager = plugin.getSlayerManager();

        // 🌟 CORRECCIÓN APLICADA: Obtenemos el lore directamente del config
        List<String> loreConfig = core.getConfigManager().getConfig("colecciones_messages.yml").getStringList("menus.slayer.item-lore");

        int slot = 10; // Empezamos a colocar los jefes en el centro de la interfaz
        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {
            if (slot >= 17) break; // Límite de seguridad visual (1 fila central)

            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name() + "</bold>"));

                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line
                                .replace("%id%", template.id())
                                .replace("%kills%", String.valueOf(template.requiredKills()))
                                .replace("%mob%", template.targetMob())
                                .replace("%boss_name%", template.bossName())))
                        .collect(Collectors.toList());
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        // Como este menú es solo el registro visual de los cazadores,
        // simplemente bloqueamos cualquier interacción para que no roben ítems.
        event.setCancelled(true);
    }
}