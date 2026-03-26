package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ProtectionFlagsMenu {

    public static void openMenu(Player player, ProtectionStone stone) {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#434343<bold>»</bold> &#FF3366Leyes del Dominio");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 27);
        Inventory inv = Bukkit.createInventory(null, tamano, titulo);

        inv.setItem(10, createFlagItem(player, Material.NETHERITE_SWORD, "Derramamiento de Sangre (PvP)", "pvp", stone));
        inv.setItem(12, createFlagItem(player, Material.ZOMBIE_HEAD, "Invasión de Pesadillas (Mobs)", "mob-spawning", stone));
        inv.setItem(14, createFlagItem(player, Material.TNT, "Devastación (Explosiones)", "tnt-damage", stone));
        inv.setItem(16, createFlagItem(player, Material.FLINT_AND_STEEL, "Llamas del Purgatorio (Fuego)", "fire-spread", stone));

        // Botón Volver
        ItemStack back = new ItemStack(Material.ENDER_PEARL);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>VOLVER AL MONOLITO</bold>"));
        back.setItemMeta(backMeta);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    private static ItemStack createFlagItem(Player player, Material mat, String nombre, String flagId, ProtectionStone stone) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        boolean activo = stone.getFlag(flagId);
        String estadoColor = activo ? "&#CC66FF[ PERMITIDO ]" : "&#FF3366[ BLOQUEADO ]";

        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>" + nombre.toUpperCase() + "</bold>"));
        meta.lore(List.of(
                CrossplayUtils.parseCrossplay(player, "&#E6CCFFLey Actual: " + estadoColor),
                CrossplayUtils.parseCrossplay(player, " "),
                CrossplayUtils.parseCrossplay(player, "&#CC66FF► Clic para alterar esta ley")
        ));
        item.setItemMeta(meta);
        return item;
    }
}