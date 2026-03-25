package me.nexo.minions.menu;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
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
        // 🌟 Título con Component Nativo de Paper para soportar HEX
        this.inv = Bukkit.createInventory(this, 36, NexoColor.parse("&#434343<bold>»</bold> &#FFAA00Terminal del Operario"));
        configurarMenu();
    }

    private void configurarMenu() {
        ItemStack cristal = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(NexoColor.parse(" "));
            cristal.setItemMeta(metaCristal);
        }
        for (int i = 0; i < 36; i++) inv.setItem(i, cristal);

        // 🌟 ÍCONOS DE GUÍA (Rol Ciberpunk)
        crearIconoGuia(1, Material.COAL, "&#FF5555🔥 Celda de Combustión", "Inyecta catalizadores térmicos aquí", "para sobrecargar el procesador del operario.", "&#FFAA00⬇ Inserte módulo debajo ⬇");
        crearIconoGuia(2, Material.DROPPER, "&#00E5FF📦 Módulo Compactador", "Conecta unidades compactadoras para", "ensamblar materiales en formato de bloque.", "&#FFAA00⬇ Inserte módulo debajo ⬇");
        crearIconoGuia(6, Material.CHEST, "&#55FF55🧰 Unidad de Expansión", "Instala expansores de memoria para", "aumentar la capacidad de almacenaje.", "&#FFAA00⬇ Inserte módulo debajo ⬇");
        crearIconoGuia(7, Material.HOPPER, "&#FFAA00🔄 Enlace Logístico", "Configura un emisor logístico para", "enviar recursos a contenedores externos.", "&#FFAA00⬇ Inserte módulo debajo ⬇");

        // Huecos de mejoras vacíos/llenos
        for (int i = 0; i < 4; i++) {
            ItemStack upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inv.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inv.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        // 📊 Estadísticas Centrales
        ItemStack stats = new ItemStack(minion.getType().getTargetMaterial());
        ItemMeta metaStats = stats.getItemMeta();
        if (metaStats != null) {
            metaStats.displayName(NexoColor.parse("&#FFAA00⭐ <bold>" + minion.getType().getDisplayName() + "</bold> &#AAAAAA(Nv. " + minion.getTier() + ")"));
            List<net.kyori.adventure.text.Component> loreStats = new ArrayList<>();
            loreStats.add(NexoColor.parse("&#AAAAAACapacidad Ocupada: &#55FF55" + minion.getStoredItems() + " uds"));

            double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
            loreStats.add(NexoColor.parse("&#AAAAAARendimiento: " + (vel > 0 ? "&#00E5FF⚡ +" + (int)vel + "%" : "&#FFAA00Estándar")));

            if (minion.tieneMejora("COMPACTOR")) loreStats.add(NexoColor.parse("&#AA00AA📦 Protocolo de Compresión [ON]"));
            if (minion.tieneMejora("STORAGE_LINK")) loreStats.add(NexoColor.parse("&#FFAA00🔄 Red Logística [CONECTADA]"));

            metaStats.lore(loreStats);
            stats.setItemMeta(metaStats);
        }
        inv.setItem(13, stats);

        // 🌟 EL BOTÓN DE EVOLUCIÓN
        ItemStack evolucion = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaEvo = evolucion.getItemMeta();
        if (metaEvo != null) {
            int sigNivel = minion.getTier() + 1;

            if (sigNivel > 12) {
                metaEvo.displayName(NexoColor.parse("&#00E5FF✨ <bold>POTENCIAL MÁXIMO ALCANZADO</bold>"));
            } else {
                metaEvo.displayName(NexoColor.parse("&#55FF55⬆ <bold>ACTUALIZAR FIRMWARE A NV. " + sigNivel + "</bold>"));
                List<net.kyori.adventure.text.Component> loreEvo = new ArrayList<>();
                loreEvo.add(NexoColor.parse("&#AAAAAAHaz clic para instalar la nueva versión en el operario."));
                loreEvo.add(NexoColor.parse(" "));
                loreEvo.add(NexoColor.parse("&#FFAA00Coste de Instalación:"));

                ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
                if (costo != null) {
                    String reqName = costo.getString("nexo_id", costo.getString("material", "Desconocido"));
                    loreEvo.add(NexoColor.parse("&#FF5555▶ " + costo.getInt("cantidad") + "x " + reqName));
                } else {
                    loreEvo.add(NexoColor.parse("&#FF5555[!] Esquema no detectado en tiers.yml"));
                }
                metaEvo.lore(loreEvo);
            }
            evolucion.setItemMeta(metaEvo);
        }
        inv.setItem(22, evolucion);

        // 🌟 EL NUEVO BOTÓN DE EXTRACCIÓN (Muestra Materiales y Experiencia)
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
            metaExtraer.displayName(NexoColor.parse("&#00E5FF📦 <bold>INICIAR EXTRACCIÓN</bold>"));

            List<net.kyori.adventure.text.Component> loreExtraer = new ArrayList<>();
            loreExtraer.add(NexoColor.parse("&#AAAAAATransfiere el inventario del operario a tu sistema."));
            loreExtraer.add(NexoColor.parse(" "));
            loreExtraer.add(NexoColor.parse("&#FFFFFFVolumen Procesado: &#FFAA00" + minion.getStoredItems() + " uds"));

            if (!tipoSkill.isEmpty() && minion.getStoredItems() > 0) {
                loreExtraer.add(NexoColor.parse("&#FFFFFFDatos de " + tipoSkill + ": &#AA00AA+" + xpAcumulada + " XP"));
            }

            loreExtraer.add(NexoColor.parse(" "));
            loreExtraer.add(NexoColor.parse("&#55FF55► ¡Clic para recolectar lote completo!"));
            metaExtraer.lore(loreExtraer);
            extraer.setItemMeta(metaExtraer);
        }
        inv.setItem(31, extraer);

        // Botón Recoger Minion
        ItemStack romper = new ItemStack(Material.BARRIER);
        ItemMeta metaRomper = romper.getItemMeta();
        if (metaRomper != null) {
            metaRomper.displayName(NexoColor.parse("&#FF5555🧨 <bold>DESMANTELAR OPERARIO</bold>"));
            romper.setItemMeta(metaRomper);
        }
        inv.setItem(35, romper);
    }

    private void crearIconoGuia(int slot, Material mat, String tituloHex, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(NexoColor.parse(tituloHex));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String linea : loreLines) {
                // Las líneas pasadas como parámetro ya llevan HEX, así que las parseamos directo
                lore.add(NexoColor.parse("&#AAAAAA" + linea.replace("&#FFAA00", "&#FFAA00")));
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