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

    // 🎨 Títulos limpios y seguros
    public static final String TITLE_PLAIN = "» Guardarropa RPG";
    public static final String MENU_TITLE = "&#555555<bold>»</bold> &#00E5FFGuardarropa RPG";

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
                meta.setDisplayName(serialize("&#FFAA00<bold>Preset de Armadura #" + presetNum + "</bold>"));
                meta.setLore(Arrays.asList(
                        serialize("&#AAAAAAGuarda o equipa conjuntos de"),
                        serialize("&#AAAAAAarmadura de forma instantánea."),
                        serialize(" "),
                        serialize("&#00E5FF▶ CLIC IZQUIERDO: &#FFFFFFEquipar Set"),
                        serialize("&#FF5555▶ CLIC DERECHO: &#FFFFFFGuardar Conjunto Actual")
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