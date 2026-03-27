package me.nexo.core.hub;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class HubMenu implements InventoryHolder {

    private final NexoCore plugin;
    private final Player player;
    private Inventory inventory;

    public HubMenu(NexoCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#9933FF🌌 Red del Nexo");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 54); // 🌟 Expandido a 54 slots para simetría
        this.inventory = Bukkit.createInventory(this, tamano, titulo);

        // 👤 CENTRO ABSOLUTO: Perfil del Jugador
        inventory.setItem(13, createPlayerProfile());

        // 🌟 FILA 1: Mecánicas Principales
        inventory.setItem(20, createButton(Material.DIAMOND_SWORD, "&#a8ff78⚔️ Habilidades", "Sube de nivel tus profesiones.", "open_skills"));
        inventory.setItem(21, createButton(Material.WRITABLE_BOOK, "&#00fbff📚 Colecciones", "Abre tu grimorio de progresión.", "open_colecciones"));
        inventory.setItem(22, createButton(Material.CRAFTING_TABLE, "&#fbd72b📖 Libro de Recetas", "Descubre crafteos de artefactos.", "open_recipes"));
        inventory.setItem(23, createButton(Material.GOLD_INGOT, "&#fbd72b📈 Bazar Global", "Comercia materiales con operarios.", "open_bazar"));
        inventory.setItem(24, createButton(Material.ZOMBIE_HEAD, "&#ff4b2b💀 Cacerías (Slayers)", "Invoca jefes y reclama sus almas.", "open_slayer"));

        // 🌟 FILA 2: Utilidades y Extras
        inventory.setItem(29, createButton(Material.ENDER_CHEST, "&#CC66FF🎒 Almacenamiento", "Abre tus mochilas remotas.", "open_pv"));
        inventory.setItem(30, createButton(Material.LEATHER_CHESTPLATE, "&#CC66FF👕 Guardarropa", "Accede a tus armaduras.", "open_wardrobe"));
        inventory.setItem(31, createButton(Material.COMPASS, "&#00fbff🌍 Viaje Rápido", "Teletransporte a zonas seguras.", "open_fast_travel"));
        inventory.setItem(32, createButton(Material.SHIELD, "&#a8ff78🛡️ Gestión de Clan", "Administra tu imperio.", "open_clans"));
        inventory.setItem(33, createButton(Material.WITHER_SKELETON_SKULL, "&#434343🌑 Mercado Negro", "Artefactos prohibidos.", "open_blackmarket"));

        // ⬛ CRISTALES DE DECORACIÓN
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaGlass = glass.getItemMeta();
        metaGlass.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(metaGlass);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, glass);
            }
        }

        player.openInventory(inventory);
    }

    private ItemStack createPlayerProfile() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player); // Pone la textura de la cabeza del jugador
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#fbd72b<bold>TUS ESTADÍSTICAS</bold>"));

        // 🌟 Aquí leerás las variables reales con PAPI o desde tu NexoUser
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#e0e0e0Operario: &#a8ff78" + player.getName()));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#ff4b2b❤ Vida: &#e0e0e0100/100"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#a8ff78🛡️ Defensa: &#e0e0e025"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#FF3366⚔️ Fuerza: &#e0e0e010"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00fbff⚡ Velocidad: &#e0e0e0100%"));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#fbd72b🪙 Monedas: &#e0e0e00.0"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#a8ff78💎 Gemas: &#e0e0e00"));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createButton(Material mat, String name, String desc, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, "<bold>" + name + "</bold>"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAA" + desc));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#a8ff78► Clic para acceder"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() { return inventory; }
}