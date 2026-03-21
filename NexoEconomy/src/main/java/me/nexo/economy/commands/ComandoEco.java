package me.nexo.economy.commands;

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
            sender.sendMessage("Este comando es solo para jugadores.");
            return true;
        }

        // 🌟 /eco (Ver tu propio dinero)
        if (args.length == 0) {
            Optional<NexoAccount> accOpt = plugin.getEconomyManager().getCachedAccount(player.getUniqueId(), NexoAccount.AccountType.PLAYER);

            if (accOpt.isPresent()) {
                NexoAccount acc = accOpt.get();
                player.sendMessage("§8=========================");
                player.sendMessage("§6§l Billetera de " + player.getName());
                player.sendMessage("§e🪙 Monedas: §f" + acc.getCoins());
                player.sendMessage("§a💎 Gemas: §f" + acc.getGems());
                player.sendMessage("§b💧 Maná: §f" + acc.getMana());
                player.sendMessage("§8=========================");
            } else {
                player.sendMessage("§cCargando tu billetera desde la base de datos...");
            }
            return true;
        }

        // 🌟 /eco give <jugador> <divisa> <cantidad> (Comando de Admin)
        if (args[0].equalsIgnoreCase("give") && player.isOp()) {
            if (args.length < 4) {
                player.sendMessage("§cUso: /eco give <jugador> <COINS|GEMS|MANA> <cantidad>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cJugador no encontrado.");
                return true;
            }

            NexoAccount.Currency currency;
            try {
                currency = NexoAccount.Currency.valueOf(args[2].toUpperCase());
            } catch (Exception e) {
                player.sendMessage("§cDivisa inválida. Usa COINS, GEMS o MANA.");
                return true;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(args[3]);
            } catch (Exception e) {
                player.sendMessage("§cCantidad inválida.");
                return true;
            }

            // ⚡ TRANSACCIÓN ATÓMICA
            plugin.getEconomyManager().updateBalanceAsync(target.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount, true).thenAccept(success -> {
                if (success) {
                    player.sendMessage("§aHas añadido " + amount + " " + currency.name() + " a " + target.getName());
                    target.sendMessage("§aHas recibido §e" + amount + " " + currency.name() + "§a.");
                } else {
                    player.sendMessage("§cError en la transacción.");
                }
            });
            return true;
        }

        player.sendMessage("§7Sub-comandos disponibles: §egive (Solo Admin)");
        return true;
    }
}