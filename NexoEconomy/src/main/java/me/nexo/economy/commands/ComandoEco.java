package me.nexo.economy.commands;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Optional;

public class ComandoEco implements CommandExecutor {

    private final NexoEconomy plugin;

    public ComandoEco(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.eco.no-jugador"));
            return true;
        }

        if (args.length == 0) {
            Optional<NexoAccount> accOpt = plugin.getEconomyManager().getCachedAccount(player.getUniqueId(), NexoAccount.AccountType.PLAYER);

            if (accOpt.isPresent()) {
                NexoAccount acc = accOpt.get();
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.divisor"));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.titulo").replace("%player%", player.getName()));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.monedas").replace("%amount%", acc.getCoins().toString()));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.gemas").replace("%amount%", String.valueOf(acc.getGems())));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.mana").replace("%amount%", String.valueOf(acc.getMana())));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.estado-cuenta.divisor"));
            } else {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.cargando"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && player.isOp()) {
            if (args.length < 4) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.errores.uso-give"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.errores.no-encontrado"));
                return true;
            }

            NexoAccount.Currency currency;
            try {
                currency = NexoAccount.Currency.valueOf(args[2].toUpperCase());
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.errores.divisa-invalida"));
                return true;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(args[3]);
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.errores.cantidad-invalida"));
                return true;
            }

            plugin.getEconomyManager().updateBalanceAsync(target.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount, true).thenAccept(success -> {
                if (success) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.exito.give")
                            .replace("%amount%", amount.toString()).replace("%currency%", currency.name()).replace("%target%", target.getName()));
                    CrossplayUtils.sendMessage(target, plugin.getConfigManager().getMessage("comandos.eco.exito.recibir")
                            .replace("%amount%", amount.toString()).replace("%currency%", currency.name()));
                } else {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.errores.tx-fallida"));
                }
            });
            return true;
        }

        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.eco.ayuda"));
        return true;
    }
}