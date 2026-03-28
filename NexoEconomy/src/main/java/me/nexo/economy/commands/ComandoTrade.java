package me.nexo.economy.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoTrade implements CommandExecutor {

    private final NexoEconomy plugin;

    // 🎨 PALETA VIVID VOID
    private static final String ERR_NOT_PLAYER = "&#8b0000[!] Acceso denegado: El terminal requiere un operario humano.";
    private static final String ERR_USAGE_MAIN = "&#8b0000[!] Uso: &#ff00ff/trade <operario> &#1c0f2ao &#ff00ff/trade accept <operario>";
    private static final String ERR_USAGE_ACCEPT = "&#8b0000[!] Uso: &#ff00ff/trade accept <operario>";
    private static final String ERR_OFFLINE = "&#8b0000[!] Operario no localizado en la red.";
    private static final String ERR_NO_REQUEST = "&#8b0000[!] No hay peticiones activas de este operario (o ya han expirado).";
    private static final String ERR_SELF = "&#8b0000[!] Negado. No puedes abrir una sesión de intercambio contigo mismo.";

    public ComandoTrade(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse(ERR_USAGE_MAIN));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(NexoColor.parse(ERR_USAGE_ACCEPT));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(NexoColor.parse(ERR_OFFLINE));
                return true;
            }

            if (plugin.getTradeManager().tienePeticionDe(player, target)) {
                plugin.getTradeManager().iniciarTrade(player, target);
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_REQUEST));
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(NexoColor.parse(ERR_OFFLINE));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(NexoColor.parse(ERR_SELF));
            return true;
        }

        plugin.getTradeManager().enviarPeticion(player, target);
        return true;
    }
}