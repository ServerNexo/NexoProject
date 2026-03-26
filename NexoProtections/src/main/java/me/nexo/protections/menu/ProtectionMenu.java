package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ProtectionMenu {

    public static void openMenu(Player player, ProtectionStone stone) {
        net.kyori.adventure.text.Component tituloCrossplay = CrossplayUtils.parseCrossplay(player, "&#434343<bold>»</bold> &#9933FFMonolito del Vacío");
        int tamanoOptimo = CrossplayUtils.getOptimizedMenuSize(player, 27);
        Inventory inv = Bukkit.createInventory(null, tamanoOptimo, tituloCrossplay);

        // SLOT 11: ACÓLITOS (MIEMBROS)
        ItemStack members = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta memMeta = members.getItemMeta();
        if (memMeta != null) {
            memMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>ACÓLITOS DEL PACTO</bold>"));
            memMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFInvoca o destierra a las almas"),
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFque tienen permitido pisar estas tierras."),
                    CrossplayUtils.parseCrossplay(player, " "),
                    CrossplayUtils.parseCrossplay(player, "&#CC66FF► Clic para gestionar acólitos")
            ));
            members.setItemMeta(memMeta);
        }
        inv.setItem(11, members);

        // SLOT 13: INFORMACIÓN DEL MONOLITO
        ItemStack info = new ItemStack(Material.LODESTONE);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>NÚCLEO DEL MONOLITO</bold>"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFSeñor Oscuro: &#FFFFFF" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName()));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFTipo de Culto: &#CC66FF" + (stone.getClanId() == null ? "Solitario" : "Sindicato")));
            lore.add(CrossplayUtils.parseCrossplay(player, " "));

            double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
            String colorEnergia = porcentaje > 50 ? "&#CC66FF" : (porcentaje > 20 ? "&#9933FF" : "&#FF3366");

            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFEsencia Consumida: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " &#FFFFFF/ &#CC66FF" + stone.getMaxEnergy()));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        // SLOT 15: LEYES DEL DOMINIO (FLAGS)
        ItemStack flags = new ItemStack(Material.SOUL_TORCH);
        ItemMeta flagMeta = flags.getItemMeta();
        if (flagMeta != null) {
            flagMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF3366<bold>LEYES DEL DOMINIO</bold>"));
            flagMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFAltera las leyes naturales"),
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFdentro de tu territorio (PvP, Fuego, etc)."),
                    CrossplayUtils.parseCrossplay(player, " "),
                    CrossplayUtils.parseCrossplay(player, "&#CC66FF► Clic para dictar las leyes")
            ));
            flags.setItemMeta(flagMeta);
        }
        inv.setItem(15, flags);

        // SLOT 22: INFUNDIR ESENCIA (RECARGA)
        ItemStack recharge = new ItemStack(Material.ECHO_SHARD);
        ItemMeta rechargeMeta = recharge.getItemMeta();
        if (rechargeMeta != null) {
            rechargeMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#CC66FF<bold>[ INFUNDIR ESENCIA ]</bold>"));
            rechargeMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFOfrece sacrificios (Diamantes)"),
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFpara alimentar el vacío del Monolito.")
            ));
            recharge.setItemMeta(rechargeMeta);
        }
        inv.setItem(22, recharge);

        // CRISTALES (Decoración Vacio)
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < tamanoOptimo; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }
}