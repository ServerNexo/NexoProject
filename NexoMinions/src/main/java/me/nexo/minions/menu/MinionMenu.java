package me.nexo.minions.menu;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.core.utils.NexoColor;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
    private final Player player;

    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(NexoMinions plugin, ActiveMinion minion, Player player) {
        this.plugin = plugin;
        this.minion = minion;
        this.player = player;

        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#1c0f2a<bold>»</bold> &#ff00ffSello del Esclavo");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 36);
        if (tamano < 36) tamano = 36;

        this.inv = Bukkit.createInventory(this, tamano, titulo);
        configurarMenu(tamano);
    }

    private void configurarMenu(int tamano) {
        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(CrossplayUtils.parseCrossplay(player, " "));
            cristal.setItemMeta(metaCristal);
        }
        for (int i = 0; i < tamano; i++) inv.setItem(i, cristal);

        crearIconoGuia(1, Material.COAL, "&#8b0000🔥 Llama Abismal", "Sacrifica combustible puro aquí", "para acelerar el tormento del esclavo.", "&#ff00ff⬇ Deposita la ofrenda ⬇");
        crearIconoGuia(2, Material.DROPPER, "&#ff00ff📦 Runas de Compresión", "Inscribe runas antiguas para", "fusionar la materia en su forma pura.", "&#ff00ff⬇ Deposita la runa ⬇");
        crearIconoGuia(6, Material.CHEST, "&#ff00ff🧰 Fauces Insaciables", "Otorga cofres y reliquias para", "expandir el estómago de esta criatura.", "&#ff00ff⬇ Deposita la mejora ⬇");
        crearIconoGuia(7, Material.HOPPER, "&#ff00ff🔄 Vínculo Umbrío", "Forja un pacto de sangre para", "drenar la materia a tus propios cofres.", "&#ff00ff⬇ Deposita el vínculo ⬇");

        for (int i = 0; i < 4; i++) {
            ItemStack upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inv.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inv.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        ItemStack stats = new ItemStack(minion.getType().getTargetMaterial());
        ItemMeta metaStats = stats.getItemMeta();
        if (metaStats != null) {
            metaStats.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff⭐ <bold>" + minion.getType().getDisplayName() + "</bold> &#1c0f2a(Nv. " + minion.getTier() + ")"));
            List<net.kyori.adventure.text.Component> loreStats = new ArrayList<>();
            loreStats.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aMateria Devorada: &#00f5ff" + minion.getStoredItems() + " uds"));
            double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
            loreStats.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aAgonía (Eficiencia): " + (vel > 0 ? "&#8b0000⚡ +" + (int)vel + "%" : "&#1c0f2aLetargo (Base)")));
            if (minion.tieneMejora("COMPACTOR")) loreStats.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff📦 Sello de Amalgama [ACTIVO]"));
            if (minion.tieneMejora("STORAGE_LINK")) loreStats.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff🔄 Nexo Logístico [CONECTADO]"));
            metaStats.lore(loreStats);
            stats.setItemMeta(metaStats);
        }
        inv.setItem(13, stats);

        ItemStack evolucion = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaEvo = evolucion.getItemMeta();
        if (metaEvo != null) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) {
                metaEvo.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff✨ <bold>CÚSPIDE DEL ABISMO ALCANZADA</bold>"));
            } else {
                metaEvo.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff⬆ <bold>ASCENDER AL VACÍO A NV. " + sigNivel + "</bold>"));
                List<net.kyori.adventure.text.Component> loreEvo = new ArrayList<>();
                loreEvo.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aRealiza el ritual para empoderar a la entidad."));
                loreEvo.add(CrossplayUtils.parseCrossplay(player, " "));
                loreEvo.add(CrossplayUtils.parseCrossplay(player, "&#ff00ffTributo Requerido:"));
                ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
                if (costo != null) {
                    String reqName = costo.getString("nexo_id", costo.getString("material", "Alma Perdida"));
                    loreEvo.add(CrossplayUtils.parseCrossplay(player, "&#8b0000▶ " + costo.getInt("cantidad") + "x " + reqName));
                } else {
                    loreEvo.add(CrossplayUtils.parseCrossplay(player, "&#8b0000[!] Ritual no detectado en tiers.yml"));
                }
                metaEvo.lore(loreEvo);
            }
            evolucion.setItemMeta(metaEvo);
        }
        inv.setItem(22, evolucion);

        String tipoMinion = minion.getType().name();
        int xpAcumulada = minion.getStoredItems() * 2;
        String tipoSkill = "";
        if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
            tipoSkill = "Agricultura";
        } else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
            tipoSkill = "Minería";
        }

        ItemStack extraer = new ItemStack(Material.HOPPER);
        ItemMeta metaExtraer = extraer.getItemMeta();
        if (metaExtraer != null) {
            metaExtraer.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff📦 <bold>COSECHAR TRIBUTO</bold>"));
            List<net.kyori.adventure.text.Component> loreExtraer = new ArrayList<>();
            loreExtraer.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aReclama toda la materia que el esclavo ha juntado."));
            loreExtraer.add(CrossplayUtils.parseCrossplay(player, " "));
            loreExtraer.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aOfrendas Listas: &#00f5ff" + minion.getStoredItems() + " uds"));
            if (!tipoSkill.isEmpty() && minion.getStoredItems() > 0) {
                loreExtraer.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aConocimiento Arcano (" + tipoSkill + "): &#ff00ff+" + xpAcumulada + " XP"));
            }
            loreExtraer.add(CrossplayUtils.parseCrossplay(player, " "));
            loreExtraer.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff► ¡Clic para reclamar tu ofrenda!"));
            metaExtraer.lore(loreExtraer);
            extraer.setItemMeta(metaExtraer);
        }
        inv.setItem(tamano - 5, extraer);

        ItemStack romper = new ItemStack(Material.BARRIER);
        ItemMeta metaRomper = romper.getItemMeta();
        if (metaRomper != null) {
            metaRomper.displayName(CrossplayUtils.parseCrossplay(player, "&#8b0000🧨 <bold>DESTERRAR ESCLAVO</bold>"));
            romper.setItemMeta(metaRomper);
        }
        inv.setItem(tamano - 1, romper);
    }

    private void crearIconoGuia(int slot, Material mat, String tituloHex, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(player, tituloHex));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String linea : loreLines) {
                lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2a" + linea));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @Override
    public Inventory getInventory() { return inv; }
    public ActiveMinion getMinion() { return minion; }
}