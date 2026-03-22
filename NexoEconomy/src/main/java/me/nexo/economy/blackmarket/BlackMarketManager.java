package me.nexo.economy.blackmarket;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.items.managers.ItemManager; // 🌟 IMPORTANTE: Conexión con NexoItems
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
        // ====================================================
        // 🌟 CATÁLOGO OSCURO (CONECTADO A NEXO ITEMS)
        // ====================================================

        // 1. Artefacto Custom: Hoja del Vacío
        possibleLootPool.add(new BlackMarketItem(
                "hoja_vacio",
                ItemManager.crearHojaVacio(),
                new BigDecimal("1500"), NexoAccount.Currency.MANA
        ));

        // 2. Material de Mejora: 16x Polvo Estelar
        ItemStack polvos = ItemManager.crearPolvoEstelar();
        polvos.setAmount(16);
        possibleLootPool.add(new BlackMarketItem(
                "polvo_estelar_x16",
                polvos,
                new BigDecimal("400"), NexoAccount.Currency.GEMS
        ));

        // 3. Libro de Encantamiento Custom (Ejemplo: Vampirismo III)
        // Usamos try-catch por si el ID del encantamiento aún no existe en tu YAML
        try {
            ItemStack libroMagico = ItemManager.generarLibroEncantamiento("vampirismo", 3);
            if (libroMagico != null && libroMagico.getType() != Material.BOOK) {
                possibleLootPool.add(new BlackMarketItem(
                        "libro_vampirismo",
                        libroMagico,
                        new BigDecimal("800"), NexoAccount.Currency.GEMS
                ));
            }
        } catch (Exception ignored) {}

        // 4. Arma RPG Oculta (Ejemplo genérico, puedes cambiar "guadana_oscura" por un ID de tu armas.yml)
        try {
            ItemStack armaProhibida = ItemManager.generarArmaRPG("guadana_oscura");
            if (armaProhibida != null && armaProhibida.getType() != Material.WOODEN_SWORD) {
                possibleLootPool.add(new BlackMarketItem(
                        "arma_rpg_oculta",
                        armaProhibida,
                        new BigDecimal("2500"), NexoAccount.Currency.MANA
                ));
            }
        } catch (Exception ignored) {}

        // 5. Relleno vainilla para dar variedad
        possibleLootPool.add(new BlackMarketItem(
                "forbidden_apple",
                crearItemMagico(Material.ENCHANTED_GOLDEN_APPLE, "§c🍎 Manzana Prohibida", "§7Fruta del inframundo."),
                new BigDecimal("150"), NexoAccount.Currency.GEMS
        ));
    }

    // ==========================================
    // 🌑 LÓGICA DE APERTURA Y CIERRE
    // ==========================================
    public void openMarket() {
        if (isMarketOpen) return;

        this.isMarketOpen = true;
        this.currentStock.clear();

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

    private ItemStack crearItemMagico(Material mat, String nombre, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}