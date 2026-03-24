package me.nexo.colecciones.menu;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.core.utils.NexoColor;
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

    public static final String TITLE_MAIN = "&#434343<bold>»</bold> &#FFAA00Categorías de Colecciones";
    public static final String TITLE_SUBMENU_PREFIX = "&#434343<bold>»</bold> &#00E5FFColecciones: &#FFAA00";
    private static final String ERR_LOADING = "&#FF5555[!] Tus datos aún están sincronizándose con la red.";

    // 🌟 1. MENÚ PRINCIPAL
    public static void abrirMenuPrincipal(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(TITLE_MAIN));

        inv.setItem(10, crearIconoCategoria(Material.IRON_PICKAXE, CollectionCategory.MINA));
        inv.setItem(11, crearIconoCategoria(Material.IRON_HOE, CollectionCategory.FARMING));
        inv.setItem(12, crearIconoCategoria(Material.IRON_AXE, CollectionCategory.TALA));
        inv.setItem(13, crearIconoCategoria(Material.IRON_SWORD, CollectionCategory.FIGHTING));
        inv.setItem(14, crearIconoCategoria(Material.FISHING_ROD, CollectionCategory.FISHING));
        inv.setItem(15, crearIconoCategoria(Material.BREWING_STAND, CollectionCategory.ALQUIMIA));
        inv.setItem(16, crearIconoCategoria(Material.ENCHANTING_TABLE, CollectionCategory.ENCANTAMIENTOS));

        player.openInventory(inv);
    }

    // 🌟 TRADUCTOR VISUAL (Para que el jugador lea en Español)
    private static String obtenerNombreCategoria(CollectionCategory cat) {
        return switch (cat) {
            case FARMING -> "Agricultura";
            case FISHING -> "Pesca";
            case FIGHTING -> "Combate";
            case MINA -> "Minería";
            case TALA -> "Tala";
            case ALQUIMIA -> "Alquimia";
            case ENCANTAMIENTOS -> "Encantamientos";
            default -> cat.name();
        };
    }

    private static ItemStack crearIconoCategoria(Material mat, CollectionCategory cat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Obtenemos el nombre traducido
            String nombreBonito = obtenerNombreCategoria(cat);
            meta.setDisplayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#55FF55<bold>" + nombreBonito + "</bold>")));

            List<String> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#AAAAAAHaz clic para explorar la")));
            lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#AAAAAAbase de datos de esta rama.")));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    // 🌟 2. SUB-MENÚ (Por Categoría)
    public static void abrirSubMenu(Player player, CollectionManager manager, CollectionCategory categoria) {
        // También traducimos el título del sub-menú
        String nombreCategoriaBonito = obtenerNombreCategoria(categoria);
        Inventory inv = Bukkit.createInventory(null, 54, NexoColor.parse(TITLE_SUBMENU_PREFIX + nombreCategoriaBonito));

        CollectionProfile profile = manager.getProfile(player.getUniqueId());

        if (profile == null) {
            player.sendMessage(NexoColor.parse(ERR_LOADING));
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
                meta.setDisplayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#FFAA00<bold>" + item.displayName() + "</bold>")));

                int cantidad = profile.getProgress(item.itemId());
                int nivel = manager.calcularNivel(cantidad);

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(NexoColor.parse("&#AAAAAAID de Registro: " + item.itemId()));
                lore.add(NexoColor.parse(" "));
                lore.add(NexoColor.parse("&#AAAAAANivel Actual: &#00E5FF" + nivel));

                if (nivel < CollectionManager.TIERS.length) {
                    int siguienteMeta = CollectionManager.TIERS[nivel];
                    lore.add(NexoColor.parse("&#AAAAAAProgreso: &#FFAA00" + cantidad + " &#AAAAAA/ &#FFAA00" + siguienteMeta));
                    lore.add(NexoColor.parse(" "));
                    lore.add(NexoColor.parse("&#55FF55¡Producción requerida para mejorar!"));
                } else {
                    lore.add(NexoColor.parse("&#AAAAAAProgreso: &#FFAA00" + cantidad));
                    lore.add(NexoColor.parse(" "));
                    lore.add(NexoColor.parse("&#FFAA00<bold>¡MÁXIMA EFICIENCIA ALCANZADA!</bold>"));
                }
                lore.add(NexoColor.parse(" "));
                lore.add(NexoColor.parse("&#AAAAAAUsa &#FFAA00/col top " + item.itemId()));
                lore.add(NexoColor.parse("&#AAAAAApara ver a los mejores del servidor."));

                meta.lore(lore);
                icono.setItemMeta(meta);
            }
            inv.addItem(icono);
        }

        // Botón de Volver
        ItemStack volver = new ItemStack(Material.ARROW);
        ItemMeta volverMeta = volver.getItemMeta();
        if (volverMeta != null) {
            volverMeta.displayName(NexoColor.parse("&#FF5555Regresar al Directorio Central"));
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