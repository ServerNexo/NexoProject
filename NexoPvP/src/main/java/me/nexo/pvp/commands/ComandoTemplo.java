package me.nexo.pvp.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.pvp.NexoPvP; // 🌟 IMPORTACIÓN AÑADIDA
import me.nexo.pvp.menus.BlessingMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoTemplo implements CommandExecutor {

    private final NexoPvP plugin;

    public ComandoTemplo() {
        this.plugin = NexoPvP.getPlugin(NexoPvP.class); // Auto-vinculación Omega
    }

    // 🌟 LECTOR DE MENSAJES
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            // 🌟 TEXTO DESDE CONFIG
            sender.sendMessage(NexoColor.parse(getMessage("mensajes.errores.solo-jugadores")));
            return true;
        }

        // 🌟 Nueva instanciación Omega
        new BlessingMenu(player).open();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);

        return true;
    }
}