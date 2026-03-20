package me.nexo.colecciones.menu;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColeccionesMenu {

    // 🌟 1. MENÚ PRINCIPAL
    public static void abrirMenuPrincipal(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Categorías de Colecciones");

        inv.setItem(10, crearIconoCategoria(Material.IRON_PICKAXE, CollectionCategory.MINA));
        inv.setItem(11, crearIconoCategoria(Material.IRON_HOE, CollectionCategory.FARMING));
        inv.setItem(12, crearIconoCategoria(Material.IRON_AXE, CollectionCategory.TALA));
        inv.setItem(13, crearIconoCategoria(Material.IRON_SWORD, CollectionCategory.FIGHTING));
        inv.setItem(14, crearIconoCategoria(Material.FISHING_ROD, CollectionCategory.FISHING));
        inv.setItem(15, crearIconoCategoria(Material.BREWING_STAND, CollectionCategory.ALQUIMIA));
        inv.setItem(16, crearIconoCategoria(Material.ENCHANTING_TABLE, CollectionCategory.ENCANTAMIENTOS));

        player.openInventory(inv);
    }

    private static ItemStack crearIconoCategoria(Material mat, CollectionCategory cat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§l" + cat.name());
            List<String> lore = new ArrayList<>();
            lore.add("§7Haz clic para ver tus");
            lore.add("§7colecciones de esta rama.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // 🌟 2. SUB-MENÚ (Por Categoría)
    public static void abrirSubMenu(Player player, CollectionManager manager, CollectionCategory categoria) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Colecciones: §8" + categoria.name());
        CollectionProfile profile = manager.getProfile(player.getUniqueId());

        if (profile == null) {
            player.sendMessage("§cTus datos aún están cargando...");
            return;
        }

        List<CollectionItem> itemsFiltrados = manager.getItemsRegistrados().values().stream()
                .filter(item -> item.category() == categoria)
                .sorted((i1, i2) -> i1.displayName().compareTo(i2.displayName()))
                .collect(Collectors.toList());

        for (CollectionItem item : itemsFiltrados) {
            Material mat = Material.matchMaterial(item.itemId());
            if (mat == null || !mat.isItem()) mat = obtenerIconoVisual(item.itemId());

            ItemStack icono = new ItemStack(mat);
            ItemMeta meta = icono.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6§l" + item.displayName());

                int cantidad = profile.getProgress(item.itemId());
                int nivel = manager.calcularNivel(cantidad);

                List<String> lore = new ArrayList<>();
                lore.add("§8ID: " + item.itemId());
                lore.add("");
                lore.add("§7Nivel Actual: §b" + nivel);

                if (nivel < CollectionManager.TIERS.length) {
                    int siguienteMeta = CollectionManager.TIERS[nivel];
                    lore.add("§7Progreso: §e" + cantidad + " §8/ §e" + siguienteMeta);
                    lore.add("");
                    lore.add("§a¡Sigue farmeando para subir!");
                } else {
                    lore.add("§7Progreso: §e" + cantidad);
                    lore.add("");
                    lore.add("§6§l¡COLECCIÓN AL MÁXIMO!");
                }
                lore.add("");
                lore.add("§7Usa §e/col top " + item.itemId());
                lore.add("§7para ver los mejores del mundo.");

                meta.setLore(lore);
                icono.setItemMeta(meta);
            }
            inv.addItem(icono);
        }

        // Botón de Volver
        ItemStack volver = new ItemStack(Material.ARROW);
        ItemMeta volverMeta = volver.getItemMeta();
        if (volverMeta != null) {
            volverMeta.setDisplayName("§cVolver atrás");
            volver.setItemMeta(volverMeta);
        }
        inv.setItem(49, volver);

        player.openInventory(inv);
    }

    private static Material obtenerIconoVisual(String id) {
        switch (id) {
            case "CARROTS": return Material.CARROT;
            case "POTATOES": return Material.POTATO;
            case "COCOA": return Material.COCOA_BEANS;
            case "BEETROOTS": return Material.BEETROOT;
            case "SWEET_BERRY_BUSH": return Material.SWEET_BERRIES;
            case "SUGAR_CANE": return Material.SUGAR_CANE;
            case "NETHER_WART": return Material.NETHER_WART;
            case "MELON": return Material.MELON;
            case "PUMPKIN": return Material.PUMPKIN;
            case "ZOMBIE": return Material.ROTTEN_FLESH;
            case "SKELETON": return Material.BONE;
            case "CREEPER": return Material.GUNPOWDER;
            case "SPIDER": return Material.SPIDER_EYE;
            case "ENDERMAN": return Material.ENDER_PEARL;
            case "BLAZE": return Material.BLAZE_ROD;
            case "SLIME": return Material.SLIME_BALL;
            default: return Material.BARRIER;
        }
    }
}