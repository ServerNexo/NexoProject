package me.nexo.pvp.commands;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import me.nexo.pvp.menus.BlessingMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoTemplo implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse("&#8b0000[!] Solo operarios humanos."));
            return true;
        }

        new BlessingMenu(NexoCore.getPlugin(NexoCore.class), player).openMenu();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);

        return true;
    }
}