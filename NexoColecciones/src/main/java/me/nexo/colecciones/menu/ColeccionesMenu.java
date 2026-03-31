package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColeccionesMenu extends NexoMenu {

    private final NexoColecciones plugin;
    private final MenuType menuType;
    private final String categoryId;
    private final String itemId;

    public enum MenuType {
        MAIN, CATEGORY, ITEM_TIERS
    }

    // 🌟 NUEVO CONSTRUCTOR UNIFICADO - SIN NEXOCORE
    public ColeccionesMenu(Player player, NexoColecciones plugin, MenuType type, String categoryId, String itemId) {
        super(player);
        this.plugin = plugin;
        this.menuType = type;
        this.categoryId = categoryId;
        this.itemId = itemId;
    }

    // 🌟 CORRECCIÓN: Usamos el ConfigManager nativo de Colecciones
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        if (menuType == MenuType.MAIN) {
            return getMessage("menus.colecciones.titulo");
        } else if (menuType == MenuType.CATEGORY) {
            CollectionCategory cat = plugin.getCollectionManager().getCategorias().get(categoryId);
            return cat != null ? cat.getNombre() : "Categorías";
        } else {
            return getMessage("menus.colecciones.tiers.titulo");
        }
    }

    @Override
    public int getSlots() {
        if (menuType == MenuType.MAIN) return 27;
        if (menuType == MenuType.CATEGORY) return 54;
        return 45; // ITEM_TIERS
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Añade el cristal morado automáticamente

        if (menuType == MenuType.MAIN) {
            for (CollectionCategory cat : plugin.getCollectionManager().getCategorias().values()) {
                ItemStack item = new ItemStack(cat.getIcono());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, cat.getNombre()));
                    List<String> loreConfig = getMessageList("menus.colecciones.categoria-lore");
                    List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                            .map(line -> CrossplayUtils.parseCrossplay(player, line))
                            .collect(Collectors.toList());
                    meta.lore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_category");
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING, cat.getId());
                    item.setItemMeta(meta);
                }
                inventory.setItem(cat.getSlot(), item);
            }

        } else if (menuType == MenuType.CATEGORY) {
            CollectionCategory cat = plugin.getCollectionManager().getCategorias().get(categoryId);
            if (cat == null) return;

            CollectionProfile profile = plugin.getCollectionManager().getProfile(player.getUniqueId());

            for (CollectionItem cItem : cat.getItems().values()) {
                int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;
                int nivelActual = plugin.getCollectionManager().calcularNivel(cItem, progreso);
                ItemStack item;
                ItemMeta meta;

                if (progreso == 0) {
                    item = new ItemStack(Material.PURPLE_DYE);
                    meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.item-desconocido.titulo")));
                        List<String> loreConfig = getMessageList("menus.colecciones.item-desconocido.lore");
                        List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                                .map(line -> CrossplayUtils.parseCrossplay(player, line))
                                .collect(Collectors.toList());
                        meta.lore(lore);
                    }
                } else {
                    item = new ItemStack(cItem.getIcono());
                    meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(CrossplayUtils.parseCrossplay(player, cItem.getNombre()));
                        List<String> loreConfig = getMessageList("menus.colecciones.item-descubierto.lore");
                        List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                                .map(line -> CrossplayUtils.parseCrossplay(player, line
                                        .replace("%level%", String.valueOf(nivelActual))
                                        .replace("%max_tier%", String.valueOf(cItem.getMaxTier()))
                                        .replace("%progress%", String.valueOf(progreso))))
                                .collect(Collectors.toList());
                        meta.lore(lore);

                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_item");
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, cItem.getId());
                    }
                }
                if (meta != null) {
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                }
                inventory.setItem(cItem.getSlotMenu(), item);
            }
            addBackButton("main");

        } else if (menuType == MenuType.ITEM_TIERS) {
            CollectionItem cItem = plugin.getCollectionManager().getItemGlobal(itemId);
            if (cItem == null) return;

            CollectionProfile profile = plugin.getCollectionManager().getProfile(player.getUniqueId());
            int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;

            int[] slotsCentro = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
            int i = 0;
            List<Integer> niveles = new ArrayList<>(cItem.getTiers().keySet());
            java.util.Collections.sort(niveles);

            for (int nivel : niveles) {
                if (i >= slotsCentro.length) break;
                Tier tier = cItem.getTier(nivel);
                boolean desbloqueado = progreso >= tier.getRequerido();
                boolean reclamado = profile != null && profile.hasClaimedTier(cItem.getId(), nivel);
                ItemStack item;
                ItemMeta meta;

                if (reclamado) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    meta = item.getItemMeta();
                    meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.tiers.reclamado.titulo").replace("%level%", String.valueOf(nivel))));
                    List<String> loreConfig = getMessageList("menus.colecciones.tiers.reclamado.lore");
                    List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                            .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%required%", String.valueOf(tier.getRequerido()))))
                            .collect(Collectors.toList());
                    meta.lore(lore);
                } else if (desbloqueado) {
                    item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                    meta = item.getItemMeta();
                    meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.tiers.listo-reclamar.titulo").replace("%level%", String.valueOf(nivel))));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    getMessageList("menus.colecciones.tiers.listo-reclamar.lore-base").forEach(line ->
                            lore.add(CrossplayUtils.parseCrossplay(player, line.replace("%required%", String.valueOf(tier.getRequerido())))));
                    tier.getLoreRecompensa().forEach(line -> lore.add(CrossplayUtils.parseCrossplay(player, line)));
                    getMessageList("menus.colecciones.tiers.listo-reclamar.lore-accion").forEach(line ->
                            lore.add(CrossplayUtils.parseCrossplay(player, line)));
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_tier");
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "tier_level"), PersistentDataType.INTEGER, nivel);
                } else {
                    item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    meta = item.getItemMeta();
                    meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.tiers.bloqueado.titulo").replace("%level%", String.valueOf(nivel))));
                    List<String> loreConfig = getMessageList("menus.colecciones.tiers.bloqueado.lore");
                    List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                            .map(line -> CrossplayUtils.parseCrossplay(player, line
                                    .replace("%progress%", String.valueOf(progreso))
                                    .replace("%required%", String.valueOf(tier.getRequerido()))))
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                inventory.setItem(slotsCentro[i], item);
                i++;
            }

            addBackButton("cat_" + cItem.getCategoriaId());

            ItemStack info = new ItemStack(Material.NETHER_STAR);
            ItemMeta iMeta = info.getItemMeta();
            iMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.tiers.stats-globales.titulo")));
            List<String> loreConfig = getMessageList("menus.colecciones.tiers.stats-globales.lore");
            List<net.kyori.adventure.text.Component> iLore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%progress%", String.valueOf(progreso))))
                    .collect(Collectors.toList());
            iMeta.lore(iLore);
            iMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "show_top");
            iMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, cItem.getId());
            info.setItemMeta(iMeta);
            inventory.setItem(40, info);
        }
    }

    private void addBackButton(String target) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.colecciones.tiers.boton-volver")));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_" + target);
        back.setItemMeta(meta);
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if (action.equals("open_category")) {
            String catId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new ColeccionesMenu(player, plugin, MenuType.CATEGORY, catId, "").open();
            }, 3L);
        }
        else if (action.equals("open_item")) {
            String iId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new ColeccionesMenu(player, plugin, MenuType.ITEM_TIERS, categoryId, iId).open();
            }, 3L);
        }
        else if (action.equals("claim_tier")) {
            Integer tierNivel = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "tier_level"), PersistentDataType.INTEGER);
            if (tierNivel != null) {
                plugin.getCollectionManager().reclamarRecompensa(player, itemId, tierNivel);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                setMenuItems();
            }
        }
        else if (action.equals("show_top")) {
            String iId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
            plugin.getCollectionManager().calcularTopAsync(player, iId);
        }
        else if (action.startsWith("back_")) {
            String target = action.replace("back_", "");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.equals("main")) {
                    new ColeccionesMenu(player, plugin, MenuType.MAIN, "", "").open();
                } else if (target.startsWith("cat_")) {
                    new ColeccionesMenu(player, plugin, MenuType.CATEGORY, target.replace("cat_", ""), "").open();
                } else {
                    new ColeccionesMenu(player, plugin, MenuType.CATEGORY, target, "").open();
                }
            }, 3L);
        }
    }
}