package me.nexo.economy.commands;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.blackmarket.BlackMarketMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoMercadoNegro implements CommandExecutor {

    private final NexoEconomy plugin;

    public ComandoMercadoNegro(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Comandos de Administrador (Forzar apertura/cierre)
        if (args.length > 0 && sender.isOp()) {
            if (args[0].equalsIgnoreCase("open")) {
                plugin.getBlackMarketManager().openMarket();
                sender.sendMessage("§aHas invocado al Mercader Oscuro.");
                return true;
            }
            if (args[0].equalsIgnoreCase("close")) {
                plugin.getBlackMarketManager().closeMarket();
                sender.sendMessage("§cHas desterrado al Mercader Oscuro.");
                return true;
            }
        }

        // Si es un jugador y pone el comando a secas, intenta abrir el menú
        if (sender instanceof Player player) {
            BlackMarketMenu.open(player, plugin);
        } else {
            sender.sendMessage("Este comando es para jugadores.");
        }

        return true;
    }
}