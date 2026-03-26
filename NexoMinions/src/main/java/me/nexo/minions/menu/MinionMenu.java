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

    // Slots dinámicos basados en la Fila 2 (índice 9 a 17) para que nunca se salgan del inventario
    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(NexoMinions plugin, ActiveMinion minion, Player player) {
        this.plugin = plugin;
        this.minion = minion;

        // 🌟 BEDROCK FIX + TEMÁTICA GÓTICA DEL VACÍO
        net.kyori.adventure.text.Component titulo = NexoColor.parse("&#434343<bold>»</bold> &#9933FFSello del Esclavo");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 36); // Base 36, Bedrock ajustará a lo que necesite

        // Medida de seguridad: Si Bedrock lo bajó a 27, forzamos a 36 porque el Minion ocupa 4 filas reales de contenido
        if (tamano < 36) tamano = 36;

        this.inv = Bukkit.createInventory(this, tamano, titulo);
        configurarMenu(tamano);
    }

    private void configurarMenu(int tamano) {
        // 🌟 Llenar fondo de cristal negro
        ItemStack cristal = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(NexoColor.parse(" "));
            cristal.setItemMeta(metaCristal);
        }
        for (int i = 0; i < tamano; i++) inv.setItem(i, cristal);

        // 🌑 ÍCONOS DE MAGIA OSCURA (Fila 1)
        crearIconoGuia(1, Material.COAL, "&#FF3366🔥 Llama Abismal", "Sacrifica combustible puro aquí", "para acelerar el tormento del esclavo.", "&#CC66FF⬇ Deposita la ofrenda ⬇");
        crearIconoGuia(2, Material.DROPPER, "&#CC66FF📦 Runas de Compresión", "Inscribe runas antiguas para", "fusionar la materia en su forma pura.", "&#CC66FF⬇ Deposita la runa ⬇");
        crearIconoGuia(6, Material.CHEST, "&#9933FF🧰 Fauces Insaciables", "Otorga cofres y reliquias para", "expandir el estómago de esta criatura.", "&#CC66FF⬇ Deposita la mejora ⬇");
        crearIconoGuia(7, Material.HOPPER, "&#CC66FF🔄 Vínculo Umbrío", "Forja un pacto de sangre para", "drenar la materia a tus propios cofres.", "&#CC66FF⬇ Deposita el vínculo ⬇");

        // 🌟 HUECOS DE MEJORAS (Fila 2)
        for (int i = 0; i < 4; i++) {
            ItemStack upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inv.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inv.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        // 📊 ESTADO DEL ESCLAVO (Slot central de la fila 2)
        ItemStack stats = new ItemStack(minion.getType().getTargetMaterial());
        ItemMeta metaStats = stats.getItemMeta();
        if (metaStats != null) {
            metaStats.displayName(NexoColor.parse("&#9933FF⭐ <bold>" + minion.getType().getDisplayName() + "</bold> &#E6CCFF(Nv. " + minion.getTier() + ")"));
            List<net.kyori.adventure.text.Component> loreStats = new ArrayList<>();
            loreStats.add(NexoColor.parse("&#E6CCFFMateria Devorada: &#CC66FF" + minion.getStoredItems() + " uds"));

            double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
            loreStats.add(NexoColor.parse("&#E6CCFFAgonía (Eficiencia): " + (vel > 0 ? "&#FF3366⚡ +" + (int)vel + "%" : "&#9933FFLetargo (Base)")));

            if (minion.tieneMejora("COMPACTOR")) loreStats.add(NexoColor.parse("&#9933FF📦 Sello de Amalgama [ACTIVO]"));
            if (minion.tieneMejora("STORAGE_LINK")) loreStats.add(NexoColor.parse("&#CC66FF🔄 Nexo Logístico [CONECTADO]"));

            metaStats.lore(loreStats);
            stats.setItemMeta(metaStats);
        }
        inv.setItem(13, stats);

        // ⬆ EL RITUAL DE ASCENSIÓN (Slot central de la fila 3)
        ItemStack evolucion = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaEvo = evolucion.getItemMeta();
        if (metaEvo != null) {
            int sigNivel = minion.getTier() + 1;

            if (sigNivel > 12) {
                metaEvo.displayName(NexoColor.parse("&#CC66FF✨ <bold>CÚSPIDE DEL ABISMO ALCANZADA</bold>"));
            } else {
                metaEvo.displayName(NexoColor.parse("&#9933FF⬆ <bold>ASCENDER AL VACÍO A NV. " + sigNivel + "</bold>"));
                List<net.kyori.adventure.text.Component> loreEvo = new ArrayList<>();
                loreEvo.add(NexoColor.parse("&#E6CCFFRealiza el ritual para empoderar a la entidad."));
                loreEvo.add(NexoColor.parse(" "));
                loreEvo.add(NexoColor.parse("&#CC66FFTributo Requerido:"));

                ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
                if (costo != null) {
                    String reqName = costo.getString("nexo_id", costo.getString("material", "Alma Perdida"));
                    loreEvo.add(NexoColor.parse("&#FF3366▶ " + costo.getInt("cantidad") + "x " + reqName));
                } else {
                    loreEvo.add(NexoColor.parse("&#FF3366[!] Ritual no detectado en tiers.yml"));
                }
                metaEvo.lore(loreEvo);
            }
            evolucion.setItemMeta(metaEvo);
        }
        inv.setItem(22, evolucion);

        // 📦 COSECHAR ALMAS (Relativo: Penúltima posición central)
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
            metaExtraer.displayName(NexoColor.parse("&#CC66FF📦 <bold>COSECHAR TRIBUTO</bold>"));

            List<net.kyori.adventure.text.Component> loreExtraer = new ArrayList<>();
            loreExtraer.add(NexoColor.parse("&#E6CCFFReclama toda la materia que el esclavo ha juntado."));
            loreExtraer.add(NexoColor.parse(" "));
            loreExtraer.add(NexoColor.parse("&#FFFFFFOfrendas Listas: &#CC66FF" + minion.getStoredItems() + " uds"));

            if (!tipoSkill.isEmpty() && minion.getStoredItems() > 0) {
                loreExtraer.add(NexoColor.parse("&#FFFFFFConocimiento Arcano (" + tipoSkill + "): &#9933FF+" + xpAcumulada + " XP"));
            }

            loreExtraer.add(NexoColor.parse(" "));
            loreExtraer.add(NexoColor.parse("&#9933FF► ¡Clic para reclamar tu ofrenda!"));
            metaExtraer.lore(loreExtraer);
            extraer.setItemMeta(metaExtraer);
        }
        inv.setItem(tamano - 5, extraer); // Posición 31 en un menú de 36

        // 🧨 DESTERRAR ESCLAVO (Relativo: Esquina inferior derecha)
        ItemStack romper = new ItemStack(Material.BARRIER);
        ItemMeta metaRomper = romper.getItemMeta();
        if (metaRomper != null) {
            metaRomper.displayName(NexoColor.parse("&#FF3366🧨 <bold>DESTERRAR ESCLAVO</bold>"));
            romper.setItemMeta(metaRomper);
        }
        inv.setItem(tamano - 1, romper); // Posición 35 en un menú de 36
    }

    private void crearIconoGuia(int slot, Material mat, String tituloHex, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(NexoColor.parse(tituloHex));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String linea : loreLines) {
                lore.add(NexoColor.parse("&#E6CCFF" + linea));
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