package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ProtectionFlagsMenu {

    public static void openMenu(Player player, ProtectionStone stone) {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#434343<bold>»</bold> &#FF3366Leyes del Dominio");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 45);
        Inventory inv = Bukkit.createInventory(null, tamano, titulo);

        // Fila 1: Entorno General
        inv.setItem(10, createFlagItem(player, Material.NETHERITE_SWORD, "Daño PvP", "pvp", stone));
        inv.setItem(11, createFlagItem(player, Material.ZOMBIE_HEAD, "Aparición de Monstruos", "mob-spawning", stone));
        inv.setItem(12, createFlagItem(player, Material.TNT, "Daño de Explosiones", "tnt-damage", stone));
        inv.setItem(13, createFlagItem(player, Material.FLINT_AND_STEEL, "Propagación de Fuego", "fire-spread", stone));
        inv.setItem(14, createFlagItem(player, Material.LEATHER, "Asesinato de Animales", "animal-damage", stone));

        // Fila 2: Interacciones de Forasteros
        inv.setItem(19, createFlagItem(player, Material.OAK_DOOR, "Uso de Puertas/Botones", "interact", stone));
        inv.setItem(20, createFlagItem(player, Material.CHEST, "Abrir Cofres/Hornos", "containers", stone));
        inv.setItem(21, createFlagItem(player, Material.HOPPER, "Robar Ítems del Suelo", "item-pickup", stone));
        inv.setItem(22, createFlagItem(player, Material.ROTTEN_FLESH, "Tirar Basura (Drop)", "item-drop", stone));

        // 🌟 PARCHE B: FLAG DE CONTROL DE ACCESO (Fronteras)
        inv.setItem(23, createFlagItem(player, Material.IRON_DOOR, "Entrada de Forasteros", "ENTRY", stone));

        // 🌟 CORRECCIÓN BEDROCK: Decoración calculada hacia atrás (Última fila)
        int inicioUltimaFila = tamano - 9;

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(gMeta);
        for (int i = inicioUltimaFila; i < tamano; i++) {
            inv.setItem(i, glass);
        }

        ItemStack back = new ItemStack(Material.ENDER_PEARL);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>VOLVER AL MONOLITO</bold>"));
        back.setItemMeta(backMeta);

        // Slot central de la última fila
        inv.setItem(tamano - 5, back);

        player.openInventory(inv);
    }

    private static ItemStack createFlagItem(Player player, Material mat, String nombre, String flagId, ProtectionStone stone) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        boolean activo = stone.getFlag(flagId);

        // Si está en false (bloqueado para forasteros), lo pintamos ROJO porque está "Protegido/Bloqueado"
        // Si está en true, lo pintamos MORADO porque está "Permitido/Peligroso"
        String estadoColor = activo ? "&#CC66FF[ PERMITIDO ]" : "&#FF3366[ BLOQUEADO ]";

        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#9933FF<bold>" + nombre.toUpperCase() + "</bold>"));
        meta.lore(List.of(
                CrossplayUtils.parseCrossplay(player, "&#E6CCFFPara forasteros: " + estadoColor),
                CrossplayUtils.parseCrossplay(player, " "),
                CrossplayUtils.parseCrossplay(player, "&#CC66FF► Clic para alternar esta ley")
        ));

        // Guardamos el ID de la flag oculta en el ítem para que el Listener sepa qué cambiar
        NamespacedKey key = new NamespacedKey(NexoProtections.getInstance(), "flag_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, flagId);

        item.setItemMeta(meta);
        return item;
    }
}