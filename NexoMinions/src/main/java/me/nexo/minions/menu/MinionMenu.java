package me.nexo.minions.menu;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.stream.Collectors;

public class MinionMenu implements InventoryHolder {
    private final Inventory inv;
    private final ActiveMinion minion;
    private final NexoMinions plugin;
    private final Player player;
    private final NexoCore core;

    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(NexoMinions plugin, ActiveMinion minion, Player player) {
        this.plugin = plugin;
        this.minion = minion;
        this.player = player;
        this.core = NexoCore.getPlugin(NexoCore.class);

        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, getMessage("menu.titulo"));
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 36);
        if (tamano < 36) tamano = 36;

        this.inv = Bukkit.createInventory(this, tamano, titulo);
        configurarMenu(tamano);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("minions_messages.yml", path);
    }

    private List<String> getMessageList(String path) {
        return core.getConfigManager().getConfig("minions_messages.yml").getStringList(path);
    }

    private void configurarMenu(int tamano) {
        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(CrossplayUtils.parseCrossplay(player, " "));
            cristal.setItemMeta(metaCristal);
        }
        for (int i = 0; i < tamano; i++) inv.setItem(i, cristal);

        crearIconoGuia(1, Material.COAL, "combustible");
        crearIconoGuia(2, Material.DROPPER, "compresion");
        crearIconoGuia(6, Material.CHEST, "almacenamiento");
        crearIconoGuia(7, Material.HOPPER, "vinculo");

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
            metaStats.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.stats.titulo")
                    .replace("%type%", minion.getType().getDisplayName())
                    .replace("%tier%", String.valueOf(minion.getTier()))));
            List<net.kyori.adventure.text.Component> loreStats = new ArrayList<>();
            loreStats.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.stats.lore.materia-devorada").replace("%items%", String.valueOf(minion.getStoredItems()))));
            double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
            String eficiencia = vel > 0 ? getMessage("menu.stats.lore.eficiencia-activa").replace("%speed%", String.valueOf((int) vel)) : getMessage("menu.stats.lore.eficiencia-base");
            loreStats.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.stats.lore.eficiencia") + eficiencia));
            if (minion.tieneMejora("COMPACTOR")) loreStats.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.stats.lore.sello-amalgama")));
            if (minion.tieneMejora("STORAGE_LINK")) loreStats.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.stats.lore.nexo-logistico")));
            metaStats.lore(loreStats);
            stats.setItemMeta(metaStats);
        }
        inv.setItem(13, stats);

        ItemStack evolucion = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaEvo = evolucion.getItemMeta();
        if (metaEvo != null) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) {
                metaEvo.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.evolucion.max-nivel")));
            } else {
                metaEvo.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.evolucion.titulo").replace("%level%", String.valueOf(sigNivel))));
                List<String> loreConfig = getMessageList("menu.evolucion.lore");
                List<net.kyori.adventure.text.Component> loreEvo = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line))
                        .collect(Collectors.toList());
                ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
                if (costo != null) {
                    String reqName = costo.getString("nexo_id", costo.getString("material", "Alma Perdida"));
                    loreEvo.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.evolucion.costo-ritual")
                            .replace("%amount%", String.valueOf(costo.getInt("cantidad")))
                            .replace("%item%", reqName)));
                } else {
                    loreEvo.add(CrossplayUtils.parseCrossplay(player, getMessage("menu.evolucion.error-ritual")));
                }
                metaEvo.lore(loreEvo);
            }
            evolucion.setItemMeta(metaEvo);
        }
        inv.setItem(22, evolucion);

        String tipoMinion = minion.getType().name();
        int xpAcumulada = minion.getStoredItems() * 2;

        // 🌟 CORRECCIÓN APLICADA: Declaración FINAL de la variable para que el Lambda no crashee.
        final String tipoSkillFinal;
        if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
            tipoSkillFinal = "Agricultura";
        } else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
            tipoSkillFinal = "Minería";
        } else {
            tipoSkillFinal = "";
        }

        ItemStack extraer = new ItemStack(Material.HOPPER);
        ItemMeta metaExtraer = extraer.getItemMeta();
        if (metaExtraer != null) {
            metaExtraer.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.cosechar.titulo")));
            List<String> loreConfig = getMessageList("menu.cosechar.lore");

            // Usamos la variable 'tipoSkillFinal' que ya es segura para el Lambda
            List<net.kyori.adventure.text.Component> loreExtraer = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line
                            .replace("%items%", String.valueOf(minion.getStoredItems()))
                            .replace("%skill%", tipoSkillFinal)
                            .replace("%xp%", String.valueOf(xpAcumulada))))
                    .collect(Collectors.toList());

            if (tipoSkillFinal.isEmpty() || minion.getStoredItems() == 0) {
                loreExtraer.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).contains("Conocimiento Arcano"));
            }
            metaExtraer.lore(loreExtraer);
            extraer.setItemMeta(metaExtraer);
        }
        inv.setItem(tamano - 5, extraer);

        ItemStack romper = new ItemStack(Material.BARRIER);
        ItemMeta metaRomper = romper.getItemMeta();
        if (metaRomper != null) {
            metaRomper.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.desterrar.titulo")));
            romper.setItemMeta(metaRomper);
        }
        inv.setItem(tamano - 1, romper);
    }

    private void crearIconoGuia(int slot, Material mat, String key) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menu.iconos-guia." + key + ".titulo")));
            List<String> loreConfig = getMessageList("menu.iconos-guia." + key + ".lore");
            List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, "&#1c0f2a" + line))
                    .collect(Collectors.toList());
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public ActiveMinion getMinion() {
        return minion;
    }
}