package me.nexo.dungeons.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.dungeons.menu.DungeonMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoDungeon implements CommandExecutor {

    private static final String ERR_NOT_PLAYER = "&#ff4b2b[!] Acceso denegado: El terminal requiere un operario humano.";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        DungeonMenu.openMainMenu(player);
        return true;
    }
}