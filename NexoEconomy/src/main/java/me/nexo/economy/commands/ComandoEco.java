package me.nexo.economy.commands;

import me.nexo.core.utils.NexoColor;
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

    // 🎨 PALETA VIVID VOID
    private static final String ERR_NOT_PLAYER = "&#8b0000[!] Acceso denegado: El terminal requiere un operario humano.";
    private static final String BC_DIVIDER = "&#1c0f2a=======================================";
    private static final String MSG_WALLET_TITLE = "&#ff00ff<bold>Estado de Cuenta: %player%</bold>";
    private static final String MSG_COINS = "&#1c0f2aBalance Operativo (Monedas): &#ff00ff🪙 %amount%";
    private static final String MSG_GEMS = "&#1c0f2aFondos Premium (Gemas): &#00f5ff💎 %amount%";
    private static final String MSG_MANA = "&#1c0f2aReserva de Maná: &#00f5ff💧 %amount%";
    private static final String MSG_LOADING = "&#ff00ff[!] Sincronizando datos con la red interbancaria...";

    private static final String ERR_USAGE = "&#8b0000[!] Error de sintaxis. Uso: &#ff00ff/eco give <operario> <COINS|GEMS|MANA> <cantidad>";
    private static final String ERR_NOT_FOUND = "&#8b0000[!] Operario no encontrado en la red.";
    private static final String ERR_CURRENCY = "&#8b0000[!] Divisa no reconocida. Opciones válidas: COINS, GEMS, MANA.";
    private static final String ERR_AMOUNT = "&#8b0000[!] Error: Cantidad numérica inválida.";

    private static final String MSG_GIVE_SUCCESS = "&#00f5ff[✓] Transferencia de %amount% %currency% a %target% completada.";
    private static final String MSG_RECEIVE = "&#00f5ff[!] Ingreso detectado: &#ff00ff%amount% %currency% &#00f5ffhan sido añadidos a tu cuenta.";
    private static final String ERR_TX_FAILED = "&#8b0000[!] Falla crítica en la transacción interbancaria.";
    private static final String MSG_HELP = "&#1c0f2aMódulos disponibles: &#ff00ffgive (Requiere Admin)";

    public ComandoEco(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length == 0) {
            Optional<NexoAccount> accOpt = plugin.getEconomyManager().getCachedAccount(player.getUniqueId(), NexoAccount.AccountType.PLAYER);

            if (accOpt.isPresent()) {
                NexoAccount acc = accOpt.get();
                player.sendMessage(NexoColor.parse(BC_DIVIDER));
                player.sendMessage(NexoColor.parse(MSG_WALLET_TITLE.replace("%player%", player.getName())));
                player.sendMessage(NexoColor.parse(MSG_COINS.replace("%amount%", acc.getCoins().toString())));
                player.sendMessage(NexoColor.parse(MSG_GEMS.replace("%amount%", String.valueOf(acc.getGems()))));
                player.sendMessage(NexoColor.parse(MSG_MANA.replace("%amount%", String.valueOf(acc.getMana()))));
                player.sendMessage(NexoColor.parse(BC_DIVIDER));
            } else {
                player.sendMessage(NexoColor.parse(MSG_LOADING));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && player.isOp()) {
            if (args.length < 4) {
                player.sendMessage(NexoColor.parse(ERR_USAGE));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(NexoColor.parse(ERR_NOT_FOUND));
                return true;
            }

            NexoAccount.Currency currency;
            try {
                currency = NexoAccount.Currency.valueOf(args[2].toUpperCase());
            } catch (Exception e) {
                player.sendMessage(NexoColor.parse(ERR_CURRENCY));
                return true;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(args[3]);
            } catch (Exception e) {
                player.sendMessage(NexoColor.parse(ERR_AMOUNT));
                return true;
            }

            plugin.getEconomyManager().updateBalanceAsync(target.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount, true).thenAccept(success -> {
                if (success) {
                    player.sendMessage(NexoColor.parse(MSG_GIVE_SUCCESS.replace("%amount%", amount.toString()).replace("%currency%", currency.name()).replace("%target%", target.getName())));
                    target.sendMessage(NexoColor.parse(MSG_RECEIVE.replace("%amount%", amount.toString()).replace("%currency%", currency.name())));
                } else {
                    player.sendMessage(NexoColor.parse(ERR_TX_FAILED));
                }
            });
            return true;
        }

        player.sendMessage(NexoColor.parse(MSG_HELP));
        return true;
    }
}