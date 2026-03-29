package me.nexo.items;

import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

public class ComandoDesguace implements CommandExecutor {

    private final NexoItems plugin;

    public ComandoDesguace(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.desguace.no-jugador"));
            return true;
        }

        Inventory inv = Bukkit.createInventory(null, 54, CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.desguace.titulo")));

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            glass.setItemMeta(glassMeta);
        }

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack btn = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            btnMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.desguace.boton.titulo")));
            btnMeta.lore(plugin.getConfigManager().getMessages().getStringList("menus.desguace.boton.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList()));
            btn.setItemMeta(btnMeta);
        }
        inv.setItem(49, btn);

        player.openInventory(inv);
        return true;
    }
}