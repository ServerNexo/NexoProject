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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BazaarMenu implements InventoryHolder {

    private final NexoEconomy plugin;
    private final Player player;
    private Inventory inventory;
    private final MenuType type;

    private String category = "";
    private String selectedItem = "";

    public enum MenuType { MAIN, CATEGORY, ITEM_OPTIONS }

    public BazaarMenu(NexoEconomy plugin, Player player, MenuType type) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
    }

    // ==========================================
    // 🏠 MENÚ 1: PRINCIPAL (Categorías)
    // ==========================================
    public void openMain() {
        this.inventory = Bukkit.createInventory(this, 54, NexoColor.parse("&#FFAA00Bazar Global"));

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

        // 📦 Botón para reclamar entregas
        ItemStack buzon = new ItemStack(Material.CHEST);
        ItemMeta metaBuzon = buzon.getItemMeta();
        metaBuzon.displayName(NexoColor.parse("&#fbd72b📦 Reclamar Entregas"));
        metaBuzon.lore(List.of(NexoColor.parse("&#AAAAAAExtrae los ítems que compraste.")));
        metaBuzon.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_deliveries");
        buzon.setItemMeta(metaBuzon);

        // 📋 NUEVO: Botón Mis Órdenes (Punto 3 - Gestión)
        ItemStack misOrdenes = new ItemStack(Material.PAPER);
        ItemMeta metaOrdenes = misOrdenes.getItemMeta();
        metaOrdenes.displayName(NexoColor.parse("&#00fbff📋 Mis Órdenes Activas"));
        metaOrdenes.lore(List.of(NexoColor.parse("&#AAAAAAGestiona o cancela tus cotizaciones.")));
        metaOrdenes.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_my_orders");
        misOrdenes.setItemMeta(metaOrdenes);

        inventory.setItem(40, buzon);
        inventory.setItem(41, misOrdenes);

        fillBorders();
        player.openInventory(inventory);
    }

    // ==========================================
    // 🗂️ MENÚ 2: CATEGORÍA ASÍNCRONA (Order Book)
    // ==========================================
    public void openCategory(String category) {
        this.category = category;
        this.inventory = Bukkit.createInventory(this, 54, NexoColor.parse("&#FFAA00Bazar - " + category));

        // 🌟 Ítem temporal mientras consulta Wall Street
        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta lMeta = loading.getItemMeta();
        lMeta.displayName(NexoColor.parse("&#AAAAAAConsultando mercado en vivo..."));
        loading.setItemMeta(lMeta);
        inventory.setItem(22, loading);

        addBackButton("main");
        fillBorders();
        player.openInventory(inventory);

        // 🌟 OPTIMIZACIÓN EXTREMA: Consulta en Hilo Secundario
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> items = new ArrayList<>();

            if (category.equals("MINING")) {
                items.add(buildBazaarItem(Material.COAL));
                items.add(buildBazaarItem(Material.IRON_INGOT));
                items.add(buildBazaarItem(Material.GOLD_INGOT));
                items.add(buildBazaarItem(Material.DIAMOND));
                items.add(buildBazaarItem(Material.OBSIDIAN));
            } else if (category.equals("FARMING")) {
                items.add(buildBazaarItem(Material.WHEAT));
                items.add(buildBazaarItem(Material.CARROT));
                items.add(buildBazaarItem(Material.POTATO));
                items.add(buildBazaarItem(Material.SUGAR_CANE));
            }

            // Vuelve al hilo principal para inyectar los ítems renderizados
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.getOpenInventory().getTitle().contains(category)) return; // Por si cerró el menú rápido

                inventory.setItem(22, new ItemStack(Material.AIR)); // Quitar reloj
                int slot = 10;
                for (ItemStack it : items) {
                    inventory.setItem(slot, it);
                    slot++;
                    if (slot == 17 || slot == 26 || slot == 35) slot += 2;
                }
                fillBorders();
            });
        });
    }

    private ItemStack buildBazaarItem(Material mat) {
        String itemId = mat.name();

        // 📊 LECTURA DE DATOS EN VIVO (Punto 2)
        BigDecimal buyPrice = plugin.getBazaarManager().getMejorPrecioVenta(itemId);
        BigDecimal sellPrice = plugin.getBazaarManager().getMejorPrecioCompra(itemId);
        int buyOrders = plugin.getBazaarManager().getVolumenOrdenes(itemId, "BUY");
        int sellOrders = plugin.getBazaarManager().getVolumenOrdenes(itemId, "SELL");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(NexoColor.parse("&#fbd72b<bold>" + mat.name() + "</bold>"));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(NexoColor.parse("&#AAAAAAPrecio de Compra: &#fbd72b" + (buyPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : buyPrice + " 🪙")));
        lore.add(NexoColor.parse("&#AAAAAAPrecio de Venta: &#fbd72b" + (sellPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : sellPrice + " 🪙")));

        // 📉 MODO FLIPPING (Punto 5)
        if (buyPrice.compareTo(BigDecimal.ZERO) > 0 && sellPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = buyPrice.subtract(sellPrice);
            lore.add(NexoColor.parse("&#AAAAAAMargen (Flipping): &#a8ff78" + margin + " 🪙"));
        }

        lore.add(NexoColor.parse(" "));
        lore.add(NexoColor.parse("&#00fbff" + buyOrders + " ítems buscando comprador (Bids)"));
        lore.add(NexoColor.parse("&#ff4b2b" + sellOrders + " ítems en venta (Asks)"));
        lore.add(NexoColor.parse(" "));
        lore.add(NexoColor.parse("&#a8ff78► Clic para ver opciones"));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_options");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);

        return item;
    }

    // ==========================================
    // ⚖️ MENÚ 3: OPCIONES (Comprar / Vender)
    // ==========================================
    public void openItemOptions(String itemId) {
        this.selectedItem = itemId;
        Material mat = Material.matchMaterial(itemId);
        if (mat == null) mat = Material.STONE;

        this.inventory = Bukkit.createInventory(this, 45, NexoColor.parse("&#FFAA00Bazar - " + itemId));

        // Botón COMPRAR
        ItemStack buy = new ItemStack(Material.GREEN_TERRACOTTA);
        ItemMeta metaBuy = buy.getItemMeta();
        metaBuy.displayName(NexoColor.parse("&#a8ff78📈 Crear Orden de Compra"));
        metaBuy.lore(List.of(NexoColor.parse("&#AAAAAACompra materiales al mercado.")));
        metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_buy_order");
        metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        buy.setItemMeta(metaBuy);

        // Botón VENDER
        ItemStack sell = new ItemStack(Material.RED_TERRACOTTA);
        ItemMeta metaSell = sell.getItemMeta();
        metaSell.displayName(NexoColor.parse("&#ff4b2b📉 Crear Orden de Venta"));
        metaSell.lore(List.of(NexoColor.parse("&#AAAAAAVende tus materiales al mercado.")));
        metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_sell_order");
        metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        sell.setItemMeta(metaSell);

        inventory.setItem(13, new ItemStack(mat));
        inventory.setItem(20, buy);
        inventory.setItem(24, sell);

        addBackButton("cat_" + category);
        fillBorders();
        player.openInventory(inventory);
    }

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