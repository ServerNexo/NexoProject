package me.nexo.dungeons.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.dungeons.NexoDungeons;
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

public class DungeonMenu extends NexoMenu {

    private final NexoDungeons plugin;

    public DungeonMenu(Player player, NexoDungeons plugin) {
        super(player);
        this.plugin = plugin;
    }

    // 🌟 CORRECCIÓN DEL ERROR: Ahora usamos el ConfigManager del propio NexoDungeons
    // Esto lee automáticamente el archivo "messages.yml" de la carpeta de NexoDungeons
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.principal.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El cristal morado automático

        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        if (instancedMeta != null) {
            instancedMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.principal.instanciadas.titulo")));
            List<String> loreConfig = getMessageList("menus.principal.instanciadas.lore");
            instancedMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            // MAGIA PDC: Guardamos la acción
            instancedMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_instanced");
            instanced.setItemMeta(instancedMeta);
        }
        inventory.setItem(11, instanced);

        ItemStack waves = new ItemStack(Material.IRON_SWORD);
        ItemMeta wavesMeta = waves.getItemMeta();
        if (wavesMeta != null) {
            wavesMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.principal.supervivencia.titulo")));
            List<String> loreConfig = getMessageList("menus.principal.supervivencia.lore");
            wavesMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            wavesMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "join_waves");
            waves.setItemMeta(wavesMeta);
        }
        inventory.setItem(13, waves);

        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        if (bossMeta != null) {
            bossMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.principal.amenazas-globales.titulo")));
            List<String> loreConfig = getMessageList("menus.principal.amenazas-globales.lore");
            bossMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            bossMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "world_boss");
            worldBoss.setItemMeta(bossMeta);
        }
        inventory.setItem(15, worldBoss);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action.equals("open_instanced")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                CrossplayUtils.sendMessage(player, getMessage("menus.listener.conectando-fortalezas"));
                player.closeInventory();

            } else if (action.equals("join_waves")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
                player.closeInventory();
                plugin.getQueueManager().addPlayerToWaves(player);

            } else if (action.equals("world_boss")) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                CrossplayUtils.sendMessage(player, getMessage("menus.listener.info-altar"));
                player.closeInventory();
            }
        }
    }
}