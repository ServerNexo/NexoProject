package me.nexo.economy.commands;

import me.nexo.core.utils.NexoColor;
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

        if (args.length > 0 && sender.isOp()) {
            if (args[0].equalsIgnoreCase("open")) {
                plugin.getBlackMarketManager().openMarket();
                sender.sendMessage(NexoColor.parse("&#8b008b[✓] Has invocado al Mercader Oscuro."));
                return true;
            }
            if (args[0].equalsIgnoreCase("close")) {
                plugin.getBlackMarketManager().closeMarket();
                sender.sendMessage(NexoColor.parse("&#ff4b2b[✓] Has desterrado al Mercader Oscuro."));
                return true;
            }
        }

        if (sender instanceof Player player) {
            BlackMarketMenu.open(player, plugin);
        } else {
            sender.sendMessage(NexoColor.parse("&#ff4b2b[!] El terminal requiere un operario humano."));
        }

        return true;
    }
}