package me.nexo.items.mochilas;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PVMenu implements InventoryHolder {

    private final NexoItems plugin;
    private final Player player;
    private Inventory inventory;

    public PVMenu(NexoItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(player, "&#CC66FF🎒 Almacenamiento (PVs)");
        int tamano = CrossplayUtils.getOptimizedMenuSize(player, 36);
        this.inventory = Bukkit.createInventory(this, tamano, titulo);

        for (int i = 1; i <= 9; i++) {
            boolean tienePerm = (i == 1) || player.hasPermission("nexo.pv." + i);

            ItemStack pvItem = new ItemStack(tienePerm ? Material.ENDER_CHEST : Material.MINECART);
            ItemMeta meta = pvItem.getItemMeta();

            meta.displayName(CrossplayUtils.parseCrossplay(player, (tienePerm ? "&#a8ff78" : "&#ff4b2b") + "<bold>Mochila " + i + "</bold>"));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (tienePerm) {
                lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAATu espacio seguro en el vacío."));
                lore.add(CrossplayUtils.parseCrossplay(player, " "));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#a8ff78► Clic para abrir"));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "pv_action"), PersistentDataType.INTEGER, i);
            } else {
                lore.add(CrossplayUtils.parseCrossplay(player, "&#AAAAAANo tienes acceso a este espacio."));
                lore.add(CrossplayUtils.parseCrossplay(player, "&#ff4b2b[!] Requiere un rango superior."));
            }
            meta.lore(lore);
            pvItem.setItemMeta(meta);

            inventory.setItem(i + 8, pvItem);
        }

        // Decoración
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaGlass = glass.getItemMeta();
        metaGlass.displayName(CrossplayUtils.parseCrossplay(player, " "));
        glass.setItemMeta(metaGlass);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) inventory.setItem(i, glass);
        }

        // ⬅️ VOLVER AL GRIMORIO
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF3366⬅ Volver al Grimorio"));
        backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, "open_hub");
        back.setItemMeta(backMeta);

        inventory.setItem(tamano - 5, back);

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}