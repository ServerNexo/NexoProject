package me.nexo.minions.menu;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.manager.ActiveMinion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MinionMenu implements InventoryHolder {
    private final Inventory inv;
    private final ActiveMinion minion;
    private final NexoMinions plugin;

    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(NexoMinions plugin, ActiveMinion minion) {
        this.plugin = plugin;
        this.minion = minion;
        this.inv = Bukkit.createInventory(this, 36, ChatColor.DARK_GRAY + "⚙️ Panel del Minion");
        configurarMenu();
    }

    private void configurarMenu() {
        ItemStack cristal = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        metaCristal.setDisplayName(" ");
        cristal.setItemMeta(metaCristal);
        for (int i = 0; i < 36; i++) inv.setItem(i, cristal);

        // 🌟 ÍCONOS DE GUÍA
        crearIconoGuia(1, Material.COAL, ChatColor.GOLD + "🔥 Ranura de Combustible", "Coloca aquí materiales para acelerar", "el tiempo de trabajo de tu minion.", "⬇️ Ponlo en el hueco de abajo ⬇️");
        crearIconoGuia(2, Material.DROPPER, ChatColor.AQUA + "📦 Ranura de Compactor", "Coloca aquí compactadores para", "convertir materiales en bloques.", "⬇️ Ponlo en el hueco de abajo ⬇️");
        crearIconoGuia(6, Material.CHEST, ChatColor.GREEN + "🧰 Ranura de Expansor", "Coloca aquí expansores para", "aumentar la mochila del minion.", "⬇️ Ponlo en el hueco de abajo ⬇️");
        crearIconoGuia(7, Material.HOPPER, ChatColor.YELLOW + "🔄 Enlace de Cofres", "Coloca aquí una tolva especial para", "enviar ítems a cofres cercanos.", "⬇️ Ponlo en el hueco de abajo ⬇️");

        // Huecos de mejoras vacíos/llenos
        for (int i = 0; i < 4; i++) {
            ItemStack upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inv.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inv.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        // Estadísticas Centrales
        ItemStack stats = new ItemStack(minion.getType().getTargetMaterial());
        ItemMeta metaStats = stats.getItemMeta();
        metaStats.setDisplayName(ChatColor.YELLOW + "⭐ " + minion.getType().getDisplayName() + " (Nv. " + minion.getTier() + ")");
        List<String> loreStats = new ArrayList<>();
        loreStats.add(ChatColor.GRAY + "Bloques en la mochila: " + ChatColor.GREEN + minion.getStoredItems() + " uds");

        double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
        loreStats.add(ChatColor.GRAY + "Velocidad: " + (vel > 0 ? ChatColor.AQUA + "⚡ +" + (int)vel + "%" : ChatColor.YELLOW + "Normal"));

        if (minion.tieneMejora("COMPACTOR")) loreStats.add(ChatColor.LIGHT_PURPLE + "📦 Compactor Activado");
        if (minion.tieneMejora("STORAGE_LINK")) loreStats.add(ChatColor.GOLD + "🔄 Guardando en Cofres");

        metaStats.setLore(loreStats);
        stats.setItemMeta(metaStats);
        inv.setItem(13, stats);

        // 🌟 EL BOTÓN DE EVOLUCIÓN
        ItemStack evolucion = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaEvo = evolucion.getItemMeta();
        int sigNivel = minion.getTier() + 1;

        if (sigNivel > 12) {
            metaEvo.setDisplayName(ChatColor.AQUA + "✨ Nivel Máximo Alcanzado");
        } else {
            metaEvo.setDisplayName(ChatColor.GREEN + "⬆️ Evolucionar a Nivel " + sigNivel);
            List<String> loreEvo = new ArrayList<>();
            loreEvo.add(ChatColor.GRAY + "Haz clic para mejorar a tu minion.");
            loreEvo.add("");
            loreEvo.add(ChatColor.YELLOW + "Costo de Mejora:");

            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo != null) {
                String reqName = costo.getString("nexo_id", costo.getString("material", "Desconocido"));
                loreEvo.add(ChatColor.RED + "▶ " + costo.getInt("cantidad") + "x " + reqName);
            } else {
                loreEvo.add(ChatColor.RED + "¡Costo no configurado en tiers.yml!");
            }
            metaEvo.setLore(loreEvo);
        }
        evolucion.setItemMeta(metaEvo);
        inv.setItem(22, evolucion);

        // 🌟 EL NUEVO BOTÓN DE EXTRACCIÓN (Muestra Materiales y Experiencia)
        String tipoMinion = minion.getType().name();
        int xpAcumulada = minion.getStoredItems() * 2; // 2 de XP por cada ítem farmeado
        String tipoSkill = "";

        if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
            tipoSkill = "Agricultura";
        } else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
            tipoSkill = "Minería";
        }

        ItemStack extraer = new ItemStack(Material.HOPPER);
        ItemMeta metaExtraer = extraer.getItemMeta();
        metaExtraer.setDisplayName(ChatColor.AQUA + "📦 Extraer Materiales");

        List<String> loreExtraer = new ArrayList<>();
        loreExtraer.add(ChatColor.GRAY + "Recoge todo lo que tu minion ha farmeado.");
        loreExtraer.add("");
        loreExtraer.add(ChatColor.WHITE + "Materiales listos: " + ChatColor.YELLOW + minion.getStoredItems() + " uds");

        if (!tipoSkill.isEmpty() && minion.getStoredItems() > 0) {
            loreExtraer.add(ChatColor.WHITE + "Experiencia (" + tipoSkill + "): " + ChatColor.LIGHT_PURPLE + "+" + xpAcumulada + " XP");
        }

        loreExtraer.add("");
        loreExtraer.add(ChatColor.GREEN + "► ¡Clic para recolectar todo!");
        metaExtraer.setLore(loreExtraer);
        extraer.setItemMeta(metaExtraer);
        inv.setItem(31, extraer);

        // Botón Recoger Minion
        ItemStack romper = new ItemStack(Material.BARRIER);
        ItemMeta metaRomper = romper.getItemMeta();
        metaRomper.setDisplayName(ChatColor.RED + "🧨 Recoger Minion");
        romper.setItemMeta(metaRomper);
        inv.setItem(35, romper);
    }

    private void crearIconoGuia(int slot, Material mat, String titulo, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(titulo);
        List<String> lore = new ArrayList<>();
        for (String linea : loreLines) lore.add(ChatColor.GRAY + linea);
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @Override
    public Inventory getInventory() { return inv; }
    public ActiveMinion getMinion() { return minion; }
}