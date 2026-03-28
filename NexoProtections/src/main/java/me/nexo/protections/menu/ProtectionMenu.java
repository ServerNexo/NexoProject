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
        // Título con protocolo Vivid Void
        net.kyori.adventure.text.Component tituloCrossplay = CrossplayUtils.parseCrossplay(player, "&#1c0f2a<bold>»</bold> &#00f5ffMonolito del Vacío");
        int tamanoOptimo = CrossplayUtils.getOptimizedMenuSize(player, 27);
        Inventory inv = Bukkit.createInventory(null, tamanoOptimo, tituloCrossplay);

        // SLOT 11: ACÓLITOS
        ItemStack members = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta memMeta = members.getItemMeta();
        if (memMeta != null) {
            memMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>ACÓLITOS DEL PACTO</bold>"));
            memMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2aExplora y destierra a las almas"),
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2avinculadas a este Monolito."),
                    CrossplayUtils.parseCrossplay(player, " "),
                    CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para abrir el registro")
            ));
            members.setItemMeta(memMeta);
        }
        inv.setItem(11, members);

        // SLOT 13: NÚCLEO
        ItemStack info = new ItemStack(Material.LODESTONE);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>NÚCLEO DEL MONOLITO</bold>"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aSeñor Oscuro: &#ff00ff" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName()));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aTipo de Culto: &#ff00ff" + (stone.getClanId() == null ? "Solitario" : "Sindicato")));
            lore.add(CrossplayUtils.parseCrossplay(player, " "));
            double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
            // Paleta Vivid Void para energía
            String colorEnergia = porcentaje > 50 ? "&#00f5ff" : (porcentaje > 20 ? "&#ff00ff" : "&#8b0000");
            lore.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aEsencia Consumida: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " &#1c0f2a/ &#00f5ff" + stone.getMaxEnergy()));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        // SLOT 15: FLAGS
        ItemStack flags = new ItemStack(Material.SOUL_TORCH);
        ItemMeta flagMeta = flags.getItemMeta();
        if (flagMeta != null) {
            flagMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>LEYES DEL DOMINIO</bold>"));
            flagMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2aAltera las leyes naturales y"),
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2afísicas de los forasteros."),
                    CrossplayUtils.parseCrossplay(player, " "),
                    CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para dictar las leyes")
            ));
            flags.setItemMeta(flagMeta);
        }
        inv.setItem(15, flags);

        // SLOT 22: RECARGA
        ItemStack recharge = new ItemStack(Material.ECHO_SHARD);
        ItemMeta rechargeMeta = recharge.getItemMeta();
        if (rechargeMeta != null) {
            rechargeMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>[ INFUNDIR ESENCIA ]</bold>"));
            rechargeMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2aOfrece sacrificios (Diamantes o Ecos)"),
                    CrossplayUtils.parseCrossplay(player, "&#1c0f2apara alimentar el vacío del Monolito.")
            ));
            recharge.setItemMeta(rechargeMeta);
        }
        inv.setItem(22, recharge);

        // DECORACIÓN con material Vivid Void
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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