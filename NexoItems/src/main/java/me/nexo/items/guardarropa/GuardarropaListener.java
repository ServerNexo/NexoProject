package me.nexo.items.guardarropa;

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

public class GuardarropaListener implements Listener {

    private final GuardarropaManager manager;
    // Título con símbolo custom para usar texturas de Nexo (\uF808 es el shift negativo clásico, \uE001 es tu textura hipotética)
    private final String TITULO_MENU = "§f\uF808\uE001§8 👕 Guardarropa RPG";

    public GuardarropaListener(GuardarropaManager manager) {
        this.manager = manager;
    }

    public void abrirMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);

        // Decoración de Slots (Ej: 3 Presets en el medio)
        int[] slotsPresets = {11, 13, 15};
        int presetNum = 1;

        for (int slot : slotsPresets) {
            ItemStack soporte = new ItemStack(Material.ARMOR_STAND);
            ItemMeta meta = soporte.getItemMeta();
            meta.setDisplayName("§e§lPreset de Armadura #" + presetNum);
            meta.setLore(List.of(
                    "§7Guarda o equipa conjuntos de",
                    "§7armadura de forma instantánea.",
                    "",
                    "§b▶ CLIC IZQUIERDO: §fEquipar",
                    "§c▶ CLIC DERECHO: §fGuardar Armadura Actual"
            ));
            soporte.setItemMeta(meta);
            inv.setItem(slot, soporte);
            presetNum++;
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1.2f);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        event.setCancelled(true); // Bloqueamos que muevan ítems del menú

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        // Mapeo de Slots a IDs de Preset
        int presetId = -1;
        if (slot == 11) presetId = 1;
        else if (slot == 13) presetId = 2;
        else if (slot == 15) presetId = 3;

        if (presetId != -1) {
            if (event.isRightClick()) {
                // GUARDAR ARMADURA
                manager.guardarPreset(p, presetId);
            } else if (event.isLeftClick()) {
                // EQUIPAR ARMADURA
                manager.equiparPreset(p, presetId);
            }
        }
    }
}