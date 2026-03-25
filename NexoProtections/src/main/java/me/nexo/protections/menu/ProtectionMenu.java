package me.nexo.protections.menu;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
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
        // 🌟 Creamos un inventario usando Component Nativo para soportar HEX en el título
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse("&#434343<bold>»</bold> &#FFAA00Nexo de Protección"));

        // Ítem central: Información de la Piedra
        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(NexoColor.parse("&#00E5FF<bold>NEXO DEL TERRITORIO</bold>"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(NexoColor.parse("&#AAAAAAPropietario: &#FFFFFF" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName()));
            lore.add(NexoColor.parse("&#AAAAAATipo de Enlace: &#FFAA00" + (stone.getClanId() == null ? "Autónomo" : "Corporativo")));
            lore.add(NexoColor.parse(" "));

            // Barra de energía visual corporativa
            double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
            String colorEnergia = porcentaje > 50 ? "&#55FF55" : (porcentaje > 20 ? "&#FFAA00" : "&#FF5555");

            lore.add(NexoColor.parse("&#AAAAAAReserva de Energía: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " &#555555/ &#55FF55" + stone.getMaxEnergy()));
            lore.add(NexoColor.parse("&#AAAAAAEstado del Escudo: " + (stone.getCurrentEnergy() > 0 ? "&#55FF55<bold>[OPERATIVO]</bold>" : "&#FF5555<bold>[VULNERABLE]</bold>")));

            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        // Botón de Recarga
        ItemStack recharge = new ItemStack(Material.EMERALD);
        ItemMeta rechargeMeta = recharge.getItemMeta();
        if (rechargeMeta != null) {
            rechargeMeta.displayName(NexoColor.parse("&#55FF55<bold>[ INYECTAR ENERGÍA ]</bold>"));
            List<net.kyori.adventure.text.Component> rechargeLore = new ArrayList<>();
            rechargeLore.add(NexoColor.parse("&#AAAAAAHaz clic aquí para transferir"));
            rechargeLore.add(NexoColor.parse("&#AAAAAAmateriales de tu inventario"));
            rechargeLore.add(NexoColor.parse("&#AAAAAAy transformarlos en energía de escudo."));
            rechargeLore.add(NexoColor.parse(" "));

            if (stone.getClanId() == null) {
                rechargeLore.add(NexoColor.parse("&#555555Combustible Aceptado: &#FFFFFFMinerales Base"));
            } else {
                rechargeLore.add(NexoColor.parse("&#555555Combustible Aceptado: &#AA00AAMateriales Refinados"));
            }
            rechargeMeta.lore(rechargeLore);
            recharge.setItemMeta(rechargeMeta);
        }
        inv.setItem(15, recharge);

        // Decoración (Cristal oscuro para evitar fatiga visual)
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(NexoColor.parse(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }
}