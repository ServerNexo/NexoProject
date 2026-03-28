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
        this.inventory = Bukkit.createInventory(this, 27, CrossplayUtils.parseCrossplay(player, "&#00f5ffGrimorio del Vacío"));
        for (CollectionCategory cat : plugin.getCollectionManager().getCategorias().values()) {
            ItemStack item = new ItemStack(cat.getIcono());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, cat.getNombre()));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aExplora las colecciones"));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2ade esta rama arcana."));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff¡Clic para abrir!"));
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
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#8b0000Ítem Desconocido"));
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aAún no has descubierto"));
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aesta materia en el mundo."));
                    meta.lore(lore);
                }
            } else {
                item = new ItemStack(cItem.getIcono());
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, cItem.getNombre()));
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aNivel Alcanzado: &#00f5ff" + nivelActual + " / " + cItem.getMaxTier()));
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProgreso Total: &#ff00ff" + progreso));
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff¡Clic para ver las recompensas!"));
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

        this.inventory = Bukkit.createInventory(this, 45, CrossplayUtils.parseCrossplay(player, "&#00f5ffTiers de Colección"));
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
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff<bold>NIVEL " + nivel + " COMPLETADO</bold>"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aRequería: &#00f5ff" + tier.getRequerido()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff[✓] El poder ya fluye en ti."));
                meta.lore(lore);
            } else if (desbloqueado) {
                item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>⭐ NIVEL " + nivel + " ALCANZADO ⭐</bold>"));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aRequería: &#00f5ff" + tier.getRequerido()));
                lore.add(net.kyori.adventure.text.Component.empty());
                for (String l : tier.getLoreRecompensa()) {
                    lore.add(CrossplayUtils.parseCrossplay(player, l));
                }
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff¡Clic para reclamar el poder!"));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_tier");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "tier_level"), PersistentDataType.INTEGER, nivel);
            } else {
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#8b0000<bold>NIVEL " + nivel + " BLOQUEADO</bold>"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProgreso: &#8b0000" + progreso + " / " + tier.getRequerido()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aMisterios aguardan a quienes"));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2ademuestren su valía..."));
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
        iMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ffEstadísticas Globales"));
        List<net.kyori.adventure.text.Component> iLore = new ArrayList<>();
        iLore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aTu progreso: &#ff00ff" + progreso));
        iLore.add(net.kyori.adventure.text.Component.empty());
        iLore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff¡Clic para ver el Top 5 en Chat!"));
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
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff⬅ Volver"));
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
    public Inventory getInventory() { return inventory; }
    public String getCategoryId() { return categoryId; }
    public String getItemId() { return itemId; }
}