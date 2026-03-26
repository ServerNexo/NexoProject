package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class ProtectionMembersMenu {

    public static void openMenu(Player player, ProtectionStone stone) {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#434343<bold>»</bold> &#9933FFAcólitos del Pacto");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 54);
        Inventory inv = Bukkit.createInventory(null, tamano, titulo);

        NamespacedKey uuidKey = new NamespacedKey(NexoProtections.getInstance(), "acolyte_uuid");
        int inicioUltimaFila = tamano - 9;

        int slot = 0;
        for (UUID uuid : stone.getTrustedFriends()) {
            if (slot >= inicioUltimaFila) break; // Evita que las cabezas pisen los botones de la última fila

            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#CC66FF<bold>" + (target.getName() != null ? target.getName() : "Alma Desconocida") + "</bold>"));
                meta.lore(List.of(
                        CrossplayUtils.parseCrossplay(player, "&#E6CCFFEsta alma tiene libre albedrío"),
                        CrossplayUtils.parseCrossplay(player, "&#E6CCFFdentro de tu Monolito."),
                        CrossplayUtils.parseCrossplay(player, " "),
                        CrossplayUtils.parseCrossplay(player, "&#FF3366► Clic para DESTERRAR esta alma")
                ));
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid.toString());
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // 🌟 CORRECCIÓN BEDROCK: Decoración calculada hacia atrás (Última fila)
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
        inv.setItem(tamano - 6, back);

        ItemStack add = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta addMeta = add.getItemMeta();
        addMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#CC66FF<bold>INVOCAR NUEVA ALMA</bold>"));
        addMeta.lore(List.of(
                CrossplayUtils.parseCrossplay(player, "&#E6CCFFPara añadir a un amigo,"),
                CrossplayUtils.parseCrossplay(player, "&#E6CCFFcierra este menú y escribe en el chat:"),
                CrossplayUtils.parseCrossplay(player, "&#9933FF/nexo trust <NombreJugador>")
        ));
        add.setItemMeta(addMeta);
        inv.setItem(tamano - 4, add);

        player.openInventory(inv);
    }
}