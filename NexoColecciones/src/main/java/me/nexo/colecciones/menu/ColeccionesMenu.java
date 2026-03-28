package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColeccionesMenu implements InventoryHolder {

    private final NexoColecciones plugin;
    private final Player player;
    private Inventory inventory;
    private final MenuType menuType;

    private String categoryId = "";
    private String itemId = "";

    public enum MenuType {
        MAIN, CATEGORY, ITEM_TIERS
    }

    public ColeccionesMenu(NexoColecciones plugin, Player player, MenuType type) {
        this.plugin = plugin;
        this.player = player;
        this.menuType = type;
    }

    public void openMain() {
        this.inventory = Bukkit.createInventory(this, 27, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.titulo")));
        for (CollectionCategory cat : plugin.getCollectionManager().getCategorias().values()) {
            ItemStack item = new ItemStack(cat.getIcono());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, cat.getNombre()));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.categoria-lore");
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
        fillBorders();
        player.openInventory(inventory);
    }

    public void openCategory(String catId) {
        this.categoryId = catId;
        CollectionCategory cat = plugin.getCollectionManager().getCategorias().get(catId);
        if (cat == null) return;

        this.inventory = Bukkit.createInventory(this, 54, CrossplayUtils.parseCrossplay(player, cat.getNombre()));
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
                    meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.item-desconocido.titulo")));
                    List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.item-desconocido.lore");
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
                    List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.item-descubierto.lore");
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
        fillBorders();
        player.openInventory(inventory);
    }

    public void openItemTiers(String itemId) {
        this.itemId = itemId;
        CollectionItem cItem = plugin.getCollectionManager().getItemGlobal(itemId);
        if (cItem == null) return;

        this.inventory = Bukkit.createInventory(this, 45, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.titulo")));
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
                meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.reclamado.titulo").replace("%level%", String.valueOf(nivel))));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.tiers.reclamado.lore");
                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%required%", String.valueOf(tier.getRequerido()))))
                        .collect(Collectors.toList());
                meta.lore(lore);
            } else if (desbloqueado) {
                item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.listo-reclamar.titulo").replace("%level%", String.valueOf(nivel))));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                plugin.getConfigManager().getMessages().getStringList("menus.colecciones.tiers.listo-reclamar.lore-base").forEach(line ->
                        lore.add(CrossplayUtils.parseCrossplay(player, line.replace("%required%", String.valueOf(tier.getRequerido())))));
                tier.getLoreRecompensa().forEach(line -> lore.add(CrossplayUtils.parseCrossplay(player, line)));
                plugin.getConfigManager().getMessages().getStringList("menus.colecciones.tiers.listo-reclamar.lore-accion").forEach(line ->
                        lore.add(CrossplayUtils.parseCrossplay(player, line)));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_tier");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "tier_level"), PersistentDataType.INTEGER, nivel);
            } else {
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.bloqueado.titulo").replace("%level%", String.valueOf(nivel))));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.tiers.bloqueado.lore");
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

        addBackButton(cItem.getCategoriaId());

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.stats-globales.titulo")));
        List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.colecciones.tiers.stats-globales.lore");
        List<net.kyori.adventure.text.Component> iLore = loreConfig.stream()
                .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%progress%", String.valueOf(progreso))))
                .collect(Collectors.toList());
        iMeta.lore(iLore);
        iMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "show_top");
        iMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, cItem.getId());
        info.setItemMeta(iMeta);
        inventory.setItem(40, info);

        fillBorders();
        player.openInventory(inventory);
    }

    private void addBackButton(String target) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.colecciones.tiers.boton-volver")));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_" + target);
        back.setItemMeta(meta);
        inventory.setItem(inventory.getSize() - 5, back);
    }

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(meta);
        for (int j = 0; j < inventory.getSize(); j++) {
            if (inventory.getItem(j) == null || inventory.getItem(j).getType().isAir()) {
                inventory.setItem(j, glass);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getItemId() {
        return itemId;
    }
}