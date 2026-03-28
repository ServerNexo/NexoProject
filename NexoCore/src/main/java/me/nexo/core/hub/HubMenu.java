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
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#ff00ff🌌 Red del Nexo");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 54);
        this.inventory = Bukkit.createInventory(this, tamano, titulo);

        inventory.setItem(13, createPlayerProfile());

        inventory.setItem(20, createButton(Material.DIAMOND_SWORD, "&#00f5ff⚔️ Habilidades", "Sube de nivel tus profesiones.", "open_skills"));
        inventory.setItem(21, createButton(Material.WRITABLE_BOOK, "&#00f5ff📚 Colecciones", "Abre tu grimorio de progresión.", "open_colecciones"));
        inventory.setItem(22, createButton(Material.CRAFTING_TABLE, "&#ff00ff📖 Libro de Recetas", "Descubre crafteos de artefactos.", "open_recipes"));
        inventory.setItem(23, createButton(Material.GOLD_INGOT, "&#ff00ff📈 Bazar Global", "Comercia materiales con operarios.", "open_bazar"));
        inventory.setItem(24, createButton(Material.ZOMBIE_HEAD, "&#8b0000💀 Cacerías (Slayers)", "Invoca jefes y reclama sus almas.", "open_slayer"));

        inventory.setItem(29, createButton(Material.ENDER_CHEST, "&#ff00ff🎒 Almacenamiento", "Abre tus mochilas remotas.", "open_pv"));
        inventory.setItem(30, createButton(Material.LEATHER_CHESTPLATE, "&#ff00ff👕 Guardarropa", "Accede a tus armaduras.", "open_wardrobe"));
        inventory.setItem(31, createButton(Material.COMPASS, "&#00f5ff🌍 Viaje Rápido", "Teletransporte a zonas seguras.", "open_fast_travel"));
        inventory.setItem(32, createButton(Material.SHIELD, "&#00f5ff🛡️ Gestión de Clan", "Administra tu imperio.", "open_clans"));
        inventory.setItem(33, createButton(Material.WITHER_SKELETON_SKULL, "&#1c0f2a🌑 Mercado Negro", "Artefactos prohibidos.", "open_blackmarket"));

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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

        meta.setOwningPlayer(player);
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>TUS ESTADÍSTICAS</bold>"));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aOperario: &#00f5ff" + player.getName()));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000❤ Vida: &#1c0f2a100/100"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff🛡️ Defensa: &#1c0f2a25"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000⚔️ Fuerza: &#1c0f2a10"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff⚡ Velocidad: &#1c0f2a100%"));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#ff00ff🪙 Monedas: &#1c0f2a0.0"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff💎 Gemas: &#1c0f2a0"));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createButton(Material mat, String name, String desc, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, "<bold>" + name + "</bold>"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a" + desc));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para acceder"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() { return inventory; }
}