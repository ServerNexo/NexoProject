package me.nexo.economy.blackmarket;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackMarketManager {

    private final NexoEconomy plugin;
    private boolean isMarketOpen = false;
    private final List<BlackMarketItem> currentStock = new ArrayList<>();

    // Catálogo maestro de cosas ilegales que el Mercader podría traer
    private final List<BlackMarketItem> possibleLootPool = new ArrayList<>();

    public BlackMarketManager(NexoEconomy plugin) {
        this.plugin = plugin;
        cargarLootPool();
    }

    private void cargarLootPool() {
        // 🚧 Aquí puedes integrar tu Addon NexoItems para traer Artefactos reales.
        // Por ahora, pondremos ejemplos genéricos con altos costos de Gemas y Maná.

        possibleLootPool.add(new BlackMarketItem(
                "corrupted_sword",
                crearItemMagico(Material.NETHERITE_SWORD, "§5⚔️ Espada Corrupta del Vacío", "§7Un arma prohibida."),
                new BigDecimal("500"), NexoAccount.Currency.GEMS
        ));

        possibleLootPool.add(new BlackMarketItem(
                "dragon_soul",
                crearItemMagico(Material.DRAGON_BREATH, "§d🐉 Alma de Dragón", "§7Contiene poder puro."),
                new BigDecimal("1000"), NexoAccount.Currency.MANA
        ));

        possibleLootPool.add(new BlackMarketItem(
                "forbidden_apple",
                crearItemMagico(Material.ENCHANTED_GOLDEN_APPLE, "§c🍎 Manzana Prohibida", "§7Te otorga vida eterna... temporalmente."),
                new BigDecimal("150"), NexoAccount.Currency.GEMS
        ));

        possibleLootPool.add(new BlackMarketItem(
                "shadow_cloak",
                crearItemMagico(Material.LEATHER_CHESTPLATE, "§8🦇 Capa de Sombras", "§7Te vuelve indetectable."),
                new BigDecimal("800"), NexoAccount.Currency.MANA
        ));
    }

    // ==========================================
    // 🌑 LÓGICA DE APERTURA Y CIERRE
    // ==========================================
    public void openMarket() {
        if (isMarketOpen) return;

        this.isMarketOpen = true;
        this.currentStock.clear();

        // Mezclamos el loot pool y tomamos 3 ítems al azar para esta rotación
        List<BlackMarketItem> shuffled = new ArrayList<>(possibleLootPool);
        Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
            currentStock.add(shuffled.get(i));
        }

        Bukkit.broadcastMessage("§8========================================");
        Bukkit.broadcastMessage("§5§l🌑 EL MERCADER OSCURO HA LLEGADO");
        Bukkit.broadcastMessage("§7Se rumorea que trae mercancía prohibida...");
        Bukkit.broadcastMessage("§8========================================");
    }

    public void closeMarket() {
        if (!isMarketOpen) return;

        this.isMarketOpen = false;
        this.currentStock.clear();

        Bukkit.broadcastMessage("§8========================================");
        Bukkit.broadcastMessage("§8§l🌑 EL MERCADER OSCURO SE HA IDO");
        Bukkit.broadcastMessage("§7Sus huellas se han desvanecido en las sombras.");
        Bukkit.broadcastMessage("§8========================================");
    }

    public boolean isMarketOpen() {
        return isMarketOpen;
    }

    public List<BlackMarketItem> getCurrentStock() {
        return currentStock;
    }

    // Utilidad rápida para crear ítems de prueba
    private ItemStack crearItemMagico(Material mat, String nombre, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}