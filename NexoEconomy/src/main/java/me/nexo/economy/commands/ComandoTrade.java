package me.nexo.economy.commands;

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
            sender.sendMessage("Este comando es solo para jugadores.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUso correcto: /trade <jugador> o /trade accept <jugador>");
            return true;
        }

        // 🌟 ACEPTAR PETICIÓN: /trade accept <jugador>
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage("§cUso correcto: /trade accept <jugador>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cEse jugador no está conectado.");
                return true;
            }

            // Verificamos en el Caché si realmente le enviaron una petición
            if (plugin.getTradeManager().tienePeticionDe(player, target)) {
                plugin.getTradeManager().iniciarTrade(player, target);
            } else {
                player.sendMessage("§cNo tienes ninguna petición pendiente de ese jugador o ya expiró.");
            }
            return true;
        }

        // 🌟 ENVIAR PETICIÓN: /trade <jugador>
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cEse jugador no está conectado.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cNo puedes intercambiar contigo mismo.");
            return true;
        }

        // Enviamos la petición usando el Manager
        plugin.getTradeManager().enviarPeticion(player, target);
        return true;
    }
}