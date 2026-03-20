package me.nexo.colecciones.menu;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ColeccionesMenu {

    public static void abrirMenu(Player player, CollectionManager manager) {
        // Creamos un inventario de 54 espacios
        Inventory inv = Bukkit.createInventory(null, 54, "§8Mis Colecciones");
        CollectionProfile profile = manager.getProfile(player.getUniqueId());

        if (profile == null) {
            player.sendMessage("§cTus datos aún están cargando...");
            return;
        }

        // 🌟 NUEVO: Tomamos la lista desordenada y la ORDENAMOS por Categoría y Nombre
        List<CollectionItem> itemsOrdenados = new ArrayList<>(manager.getItemsRegistrados().values());
        itemsOrdenados.sort((item1, item2) -> {
            int categoriaCompare = item1.category().name().compareTo(item2.category().name());
            if (categoriaCompare != 0) {
                return categoriaCompare; // Primero ordenamos por Categoría
            }
            return item1.displayName().compareTo(item2.displayName()); // Luego por Nombre
        });

        // Iteramos sobre la lista ya ordenada
        for (CollectionItem item : itemsOrdenados) {
            Material mat = Material.matchMaterial(item.itemId());

            if (mat == null || !mat.isItem()) {
                mat = obtenerIconoVisual(item.itemId());
            }

            ItemStack icono = new ItemStack(mat);
            ItemMeta meta = icono.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6§l" + item.displayName());

                int cantidad = profile.getProgress(item.itemId());
                int nivel = manager.calcularNivel(cantidad);

                List<String> lore = new ArrayList<>();
                lore.add("§8Categoría: " + item.category().name());
                lore.add("");
                lore.add("§7Nivel Actual: §b" + nivel);

                if (nivel < CollectionManager.TIERS.length) {
                    int siguienteMeta = CollectionManager.TIERS[nivel];
                    lore.add("§7Progreso: §e" + cantidad + " §8/ §e" + siguienteMeta);
                    lore.add("");
                    lore.add("§a¡Sigue farmeando para subir de nivel!");
                } else {
                    lore.add("§7Progreso: §e" + cantidad);
                    lore.add("");
                    lore.add("§6§l¡COLECCIÓN AL MÁXIMO!");
                }

                meta.setLore(lore);
                icono.setItemMeta(meta);
            }
            inv.addItem(icono);
        }

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