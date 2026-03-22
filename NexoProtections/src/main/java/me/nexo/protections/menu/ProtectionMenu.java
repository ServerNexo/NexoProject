package me.nexo.protections.menu;

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
        // Creamos un inventario de 27 espacios (3 filas)
        Inventory inv = Bukkit.createInventory(null, 27, "§8Piedra de Protección");

        // Ítem central: Información de la Piedra
        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§b§lNexo del Territorio");
        List<String> lore = new ArrayList<>();
        lore.add("§7Dueño: §f" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName());
        lore.add("§7Tipo: §e" + (stone.getClanId() == null ? "Solitario" : "Clan"));
        lore.add("");

        // Barra de energía visual
        double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
        String colorEnergia = porcentaje > 50 ? "§a" : (porcentaje > 20 ? "§e" : "§c");
        lore.add("§7Energía: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " §8/ §a" + stone.getMaxEnergy());
        lore.add("§7Estado: " + (stone.getCurrentEnergy() > 0 ? "§a§lACTIVO" : "§c§lVULNERABLE"));

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        // Botón de Recarga
        ItemStack recharge = new ItemStack(Material.EMERALD);
        ItemMeta rechargeMeta = recharge.getItemMeta();
        rechargeMeta.setDisplayName("§a§l[ Recargar Energía ]");
        List<String> rechargeLore = new ArrayList<>();
        rechargeLore.add("§7Haz clic aquí para sacrificar");
        rechargeLore.add("§7materiales de tu inventario");
        rechargeLore.add("§7y transformarlos en energía.");
        rechargeLore.add("");
        if (stone.getClanId() == null) {
            rechargeLore.add("§8Coste: §fCarbón, Hierro, Oro o Diamante.");
        } else {
            rechargeLore.add("§8Coste: §dMateriales Refinados (NexoItems)");
        }
        rechargeMeta.setLore(rechargeLore);
        recharge.setItemMeta(rechargeMeta);
        inv.setItem(15, recharge);

        // Decoración
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }
}