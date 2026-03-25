package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils; // 🌟 TRADUCTOR UNIVERSAL
import me.nexo.core.utils.NexoColor;
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
        // 🌟 CROSS-PLAY: Parseamos el título del inventario usando el Traductor Universal
        net.kyori.adventure.text.Component tituloCrossplay = CrossplayUtils.parseCrossplay(player, "&#434343<bold>»</bold> &#FFAA00Nexo de Protección");

        // 🌟 CROSS-PLAY: Escalamos dinámicamente si es Bedrock (aunque 27 es safe, lo dejamos preparado para el futuro)
        int tamanoOptimo = CrossplayUtils.getOptimizedMenuSize(player, 27);
        Inventory inv = Bukkit.createInventory(null, tamanoOptimo, tituloCrossplay);

        // Ítem central: Información de la Piedra
        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#00E5FF<bold>NEXO DEL TERRITORIO</bold>"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAPropietario: &#FFFFFF" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName()));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAATipo de Enlace: &#FFAA00" + (stone.getClanId() == null ? "Autónomo" : "Corporativo")));
            lore.add(CrossplayUtils.parseCrossplay(player, " "));

            // Barra de energía visual corporativa
            double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
            String colorEnergia = porcentaje > 50 ? "&#55FF55" : (porcentaje > 20 ? "&#FFAA00" : "&#FF5555");

            lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAReserva de Energía: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " &#555555/ &#55FF55" + stone.getMaxEnergy()));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAEstado del Escudo: " + (stone.getCurrentEnergy() > 0 ? "&#55FF55<bold>[OPERATIVO]</bold>" : "&#FF5555<bold>[VULNERABLE]</bold>")));

            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        // Botón de Recarga
        ItemStack recharge = new ItemStack(Material.EMERALD);
        ItemMeta rechargeMeta = recharge.getItemMeta();
        if (rechargeMeta != null) {
            rechargeMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#55FF55<bold>[ INYECTAR ENERGÍA ]</bold>"));
            List<net.kyori.adventure.text.Component> rechargeLore = new ArrayList<>();
            rechargeLore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAHaz clic aquí para transferir"));
            rechargeLore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAmateriales de tu inventario"));
            rechargeLore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAAy transformarlos en energía de escudo."));
            rechargeLore.add(CrossplayUtils.parseCrossplay(player, " "));

            if (stone.getClanId() == null) {
                rechargeLore.add(CrossplayUtils.parseCrossplay(player, "&#555555Combustible Aceptado: &#FFFFFFMinerales Base"));
            } else {
                rechargeLore.add(CrossplayUtils.parseCrossplay(player, "&#555555Combustible Aceptado: &#AA00AAMateriales Refinados"));
            }
            rechargeMeta.lore(rechargeLore);
            recharge.setItemMeta(rechargeMeta);
        }
        inv.setItem(15, recharge);

        // Decoración (Cristal oscuro para evitar fatiga visual)
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