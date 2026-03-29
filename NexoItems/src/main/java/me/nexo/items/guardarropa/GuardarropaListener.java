package me.nexo.items.guardarropa;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class GuardarropaListener implements Listener {

    private final NexoItems plugin;
    private final GuardarropaManager manager;

    public GuardarropaListener(NexoItems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuardarropaManager();
    }

    public void abrirMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(p, plugin.getConfigManager().getMessage("menus.guardarropa.titulo")));

        int[] slotsPresets = {11, 13, 15};
        int presetNum = 1;

        for (int slot : slotsPresets) {
            ItemStack soporte = new ItemStack(Material.ARMOR_STAND);
            ItemMeta meta = soporte.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(p, plugin.getConfigManager().getMessage("menus.guardarropa.preset.titulo").replace("%id%", String.valueOf(presetNum))));
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.guardarropa.preset.lore");
                meta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(p, line)).collect(Collectors.toList()));
                soporte.setItemMeta(meta);
            }
            inv.setItem(slot, soporte);
            presetNum++;
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1.2f);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(plugin.getConfigManager().getMessage("menus.guardarropa.titulo").replaceAll("<[^>]*>", ""))) return;

        event.setCancelled(true);

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        int presetId = -1;
        if (slot == 11) presetId = 1;
        else if (slot == 13) presetId = 2;
        else if (slot == 15) presetId = 3;

        if (presetId != -1) {
            if (event.isRightClick()) {
                manager.guardarPreset(p, presetId);
            } else if (event.isLeftClick()) {
                manager.equiparPreset(p, presetId);
            }
        }
    }
}