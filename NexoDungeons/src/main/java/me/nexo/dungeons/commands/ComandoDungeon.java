package me.nexo.dungeons.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.menu.DungeonMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoDungeon implements CommandExecutor {

    private final NexoDungeons plugin;
    private static final String ERR_NOT_PLAYER = "&#ff4b2b[!] Acceso denegado: El terminal requiere un operario humano.";

    public ComandoDungeon(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        // 🌟 CORRECCIÓN: Invertimos el orden para que encaje perfectamente con tu menú
        DungeonMenu.openMainMenu(player, plugin);
        return true;
    }
}