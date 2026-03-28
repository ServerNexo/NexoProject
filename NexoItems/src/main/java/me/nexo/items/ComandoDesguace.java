package me.nexo.items;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ComandoDesguace implements CommandExecutor {

    private static final String ERR_NOT_PLAYER = "&#8b0000[!] El terminal requiere un operario humano.";
    public static final String TITLE_PLAIN = "» Desguace Automático";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#8b0000Desguace Automático";
    private static final String ITEM_NAME = "&#8b0000<bold>⚡ Iniciar Desguace</bold>";
    private static final String ITEM_LORE_1 = "&#1c0f2aHaz clic para desguazar todos los ítems";
    private static final String ITEM_LORE_2 = "&#1c0f2aválidos que hayas introducido.";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        Inventory inv = Bukkit.createInventory(null, 54, NexoColor.parse(MENU_TITLE));

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack btn = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            btnMeta.setDisplayName(serialize(ITEM_NAME));
            btnMeta.setLore(java.util.Arrays.asList(serialize(ITEM_LORE_1), serialize(ITEM_LORE_2)));
            btn.setItemMeta(btnMeta);
        }
        inv.setItem(49, btn);

        player.openInventory(inv);
        return true;
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }
}