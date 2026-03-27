package me.nexo.economy.bazar;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
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

public class BazaarMenu implements InventoryHolder {

    private final NexoEconomy plugin;
    private final Player player;
    private Inventory inventory;
    private final MenuType type;

    // Variables de contexto
    private String category = "";
    private String selectedItem = "";

    public enum MenuType {
        MAIN, CATEGORY, ITEM_OPTIONS
    }

    public BazaarMenu(NexoEconomy plugin, Player player, MenuType type) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
    }

    // ==========================================
    // 🏠 MENÚ 1: PRINCIPAL (Categorías)
    // ==========================================
    public void openMain() {
        this.inventory = Bukkit.createInventory(this, 45, NexoColor.parse("&#FFAA00Bazar Global"));

        // Ejemplo: Categoría Minería
        ItemStack mineria = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = mineria.getItemMeta();
        meta.displayName(NexoColor.parse("&#00fbffMinería y Minerales"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_category");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "category"), PersistentDataType.STRING, "MINING");
        mineria.setItemMeta(meta);

        // Ejemplo: Categoría Agricultura
        ItemStack farmeo = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta2 = farmeo.getItemMeta();
        meta2.displayName(NexoColor.parse("&#a8ff78Agricultura y Botánica"));
        meta2.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta2.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_category");
        meta2.getPersistentDataContainer().set(new NamespacedKey(plugin, "category"), PersistentDataType.STRING, "FARMING");
        farmeo.setItemMeta(meta2);

        inventory.setItem(20, mineria);
        inventory.setItem(24, farmeo);

        // Botón para reclamar entregas (Buzón)
        ItemStack buzon = new ItemStack(Material.CHEST);
        ItemMeta metaBuzon = buzon.getItemMeta();
        metaBuzon.displayName(NexoColor.parse("&#fbd72b📦 Reclamar Entregas"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(NexoColor.parse("&#AAAAAAHaz clic para extraer los ítems"));
        lore.add(NexoColor.parse("&#AAAAAAque compraste o no se vendieron."));
        metaBuzon.lore(lore);
        metaBuzon.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_deliveries");
        buzon.setItemMeta(metaBuzon);

        inventory.setItem(40, buzon);

        fillBorders();
        player.openInventory(inventory);
    }

    // ==========================================
    // 🗂️ MENÚ 2: CATEGORÍA (Ítems)
    // ==========================================
    public void openCategory(String category) {
        this.category = category;
        this.inventory = Bukkit.createInventory(this, 54, NexoColor.parse("&#FFAA00Bazar - " + category));

        // Esto es un ejemplo. Lo ideal es leer los ítems desde un config.yml del Bazar
        if (category.equals("MINING")) {
            addItemToMenu(Material.COAL, 10);
            addItemToMenu(Material.IRON_INGOT, 11);
            addItemToMenu(Material.GOLD_INGOT, 12);
            addItemToMenu(Material.DIAMOND, 13);
            addItemToMenu(Material.OBSIDIAN, 14);
        } else if (category.equals("FARMING")) {
            addItemToMenu(Material.WHEAT, 10);
            addItemToMenu(Material.CARROT, 11);
            addItemToMenu(Material.POTATO, 12);
            addItemToMenu(Material.SUGAR_CANE, 13);
        }

        addBackButton("main");
        fillBorders();
        player.openInventory(inventory);
    }

    private void addItemToMenu(Material mat, int slot) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(NexoColor.parse("&#fbd72b" + mat.name()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(NexoColor.parse("&#AAAAAAHaz clic para ver las opciones"));
        lore.add(NexoColor.parse("&#AAAAAAde compra y venta."));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_options");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, mat.name());
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    // ==========================================
    // ⚖️ MENÚ 3: OPCIONES (Comprar / Vender)
    // ==========================================
    public void openItemOptions(String itemId) {
        this.selectedItem = itemId;
        Material mat = Material.matchMaterial(itemId);
        if (mat == null) mat = Material.STONE;

        this.inventory = Bukkit.createInventory(this, 45, NexoColor.parse("&#FFAA00Bazar - " + itemId));

        // Botón VENDER
        ItemStack sell = new ItemStack(Material.RED_TERRACOTTA);
        ItemMeta metaSell = sell.getItemMeta();
        metaSell.displayName(NexoColor.parse("&#ff4b2b📉 Crear Orden de Venta"));
        List<net.kyori.adventure.text.Component> loreSell = new ArrayList<>();
        loreSell.add(NexoColor.parse("&#AAAAAAVende tus materiales a la red."));
        loreSell.add(NexoColor.parse("&#ff4b2b[!] Requiere Nivel 1 de Colección"));
        metaSell.lore(loreSell);
        metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_sell_order");
        metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        sell.setItemMeta(metaSell);

        // Botón COMPRAR
        ItemStack buy = new ItemStack(Material.GREEN_TERRACOTTA);
        ItemMeta metaBuy = buy.getItemMeta();
        metaBuy.displayName(NexoColor.parse("&#a8ff78📈 Crear Orden de Compra"));
        List<net.kyori.adventure.text.Component> loreBuy = new ArrayList<>();
        loreBuy.add(NexoColor.parse("&#AAAAAACompra materiales a otros operarios."));
        loreBuy.add(NexoColor.parse("&#ff4b2b[!] Requiere Nivel 1 de Colección"));
        metaBuy.lore(loreBuy);
        metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_buy_order");
        metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        buy.setItemMeta(metaBuy);

        // Mostrar el ítem en el centro
        inventory.setItem(13, new ItemStack(mat));
        inventory.setItem(20, buy);
        inventory.setItem(24, sell);

        addBackButton("cat_" + category);
        fillBorders();
        player.openInventory(inventory);
    }

    // ==========================================
    // 🛠️ HERRAMIENTAS VISUALES
    // ==========================================
    private void addBackButton(String target) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(NexoColor.parse("&#FF3366⬅ Volver"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_" + target);
        back.setItemMeta(meta);
        inventory.setItem(inventory.getSize() - 5, back);
    }

    private void fillBorders() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.empty());
        glass.setItemMeta(meta);

        for (int j = 0; j < inventory.getSize(); j++) {
            if (inventory.getItem(j) == null || inventory.getItem(j).getType().isAir()) {
                inventory.setItem(j, glass);
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public String getCategory() { return category; }
}