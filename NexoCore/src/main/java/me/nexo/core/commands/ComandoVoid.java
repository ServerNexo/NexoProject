package me.nexo.core.commands;

import me.nexo.core.NexoCore;
import me.nexo.core.menus.VoidBlessingMenu;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoVoid implements CommandExecutor {

    private final NexoCore plugin;

    public ComandoVoid(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse("&#8b0000[!] Módulo reservado para entidades humanas."));
            return true;
        }

        if (!player.hasPermission("nexocore.commands.void")) {
            player.sendMessage(NexoColor.parse("&#ff0000[!] No tienes permiso para invocar el Vacío."));
            return true;
        }

        // Abrimos el menú y reproducimos un sonido resonante de amatista
        new VoidBlessingMenu(plugin, player).openMenu();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);

        return true;
    }
}