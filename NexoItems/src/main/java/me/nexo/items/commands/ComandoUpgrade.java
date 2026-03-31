package me.nexo.items.commands;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.estaciones.UpgradeMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoUpgrade implements CommandExecutor {

    private final NexoItems plugin;

    public ComandoUpgrade(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Solo los operarios humanos pueden abrir la Forja."));
            return true;
        }

        // Si quieres que solo VIPs o Admins la abran con comando, descomenta esto:
        // if (!player.hasPermission("nexo.forja.comando")) {
        //     CrossplayUtils.sendMessage(player, "&#FF5555[!] No tienes permiso para invocar la Forja remotamente.");
        //     return true;
        // }

        // 🌟 Invocamos nuestra nueva interfaz Omega
        new UpgradeMenu(player, plugin).open();
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);

        return true;
    }
}