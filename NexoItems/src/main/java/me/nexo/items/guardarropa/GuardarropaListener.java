package me.nexo.items.guardarropa;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

import java.util.Arrays;

public class GuardarropaListener implements Listener {

    private final GuardarropaManager manager;

    public static final String TITLE_PLAIN = "» Guardarropa RPG";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#00f5ffGuardarropa RPG";

    public GuardarropaListener(GuardarropaManager manager) {
        this.manager = manager;
    }

    public void abrirMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        int[] slotsPresets = {11, 13, 15};
        int presetNum = 1;

        for (int slot : slotsPresets) {
            ItemStack soporte = new ItemStack(Material.ARMOR_STAND);
            ItemMeta meta = soporte.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(serialize("&#ff00ff<bold>Preset de Armadura #" + presetNum + "</bold>"));
                meta.setLore(Arrays.asList(
                        serialize("&#1c0f2aGuarda o equipa conjuntos de"),
                        serialize("&#1c0f2aarmadura de forma instantánea."),
                        serialize(" "),
                        serialize("&#00f5ff▶ CLIC IZQUIERDO: &#1c0f2aEquipar Set"),
                        serialize("&#8b0000▶ CLIC DERECHO: &#1c0f2aGuardar Conjunto Actual")
                ));
                soporte.setItemMeta(meta);
            }
            inv.setItem(slot, soporte);
            presetNum++;
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1.2f);
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(TITLE_PLAIN)) return;

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