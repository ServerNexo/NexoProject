package me.nexo.economy.commands;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoTrade implements CommandExecutor {

    private final NexoEconomy plugin;

    public ComandoTrade(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.trade.errores.no-jugador"));
            return true;
        }

        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.uso-principal"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.uso-accept"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.offline"));
                return true;
            }

            if (plugin.getTradeManager().tienePeticionDe(player, target)) {
                plugin.getTradeManager().iniciarTrade(player, target);
            } else {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.sin-peticion"));
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.offline"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.trade.errores.auto-trade"));
            return true;
        }

        plugin.getTradeManager().enviarPeticion(player, target);
        return true;
    }
}