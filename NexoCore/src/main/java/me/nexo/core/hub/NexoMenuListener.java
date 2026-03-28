package me.nexo.core.hub;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NexoMenuListener implements Listener {

    private final NexoCore plugin;
    private final NamespacedKey menuKey;

    public NexoMenuListener(NexoCore plugin) {
        this.plugin = plugin;
        this.menuKey = new NamespacedKey(plugin, "is_nexo_menu");
    }

    public ItemStack getMenuItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(NexoColor.parse("&#ff00ff<bold>Menú Principal</bold> &#1c0f2a(Clic Derecho)"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(NexoColor.parse("&#1c0f2aTu conexión directa con el Nexo."));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(NexoColor.parse("&#00f5ff¡Clic derecho para abrir!"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { giveMenu(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> giveMenu(event.getPlayer()), 2L);
    }

    private void giveMenu(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuItem(item)) player.getInventory().setItem(i, null);
        }
        player.getInventory().setItem(8, getMenuItem());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isMenuItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemStack drop = iter.next();
            if (isMenuItem(drop)) iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

            ItemStack item = event.getItem();
            if (isMenuItem(item)) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

                player.closeInventory();
                new HubMenu(plugin, player).openMenu();
            }
        }
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && isMenuItem(event.getCurrentItem())) event.setCancelled(true);
        if (event.getCursor() != null && isMenuItem(event.getCursor())) event.setCancelled(true);
        if (event.getClick().name().contains("NUMBER_KEY")) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (isMenuItem(hotbarItem)) event.setCancelled(true);
        }

        if (event.getInventory().getHolder() instanceof HubMenu) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ItemMeta meta = event.getCurrentItem().getItemMeta();
            NamespacedKey actionKey = new NamespacedKey(plugin, "hub_action");

            if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
                String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                player.closeInventory();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    switch (action) {
                        case "open_skills":
                            player.performCommand("skills");
                            break;
                        case "open_colecciones":
                            player.performCommand("colecciones");
                            break;
                        case "open_recipes":
                            player.performCommand("recipes");
                            break;
                        case "open_bazar":
                            player.performCommand("bazar");
                            break;
                        case "open_slayer":
                            player.performCommand("slayer");
                            break;
                        case "open_pv":
                            player.performCommand("pv");
                            break;
                        case "open_wardrobe":
                            player.performCommand("wardrobe");
                            break;
                        case "open_fast_travel":
                            player.performCommand("warp");
                            break;
                        case "open_clans":
                            player.performCommand("clan");
                            break;
                        case "open_blackmarket":
                            player.performCommand("mercadonegro");
                            break;
                    }
                }, 3L);
            }
        }
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }
}