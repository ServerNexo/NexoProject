package me.nexo.items.mochilas;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ComandoPV implements CommandExecutor {

    private final NexoItems plugin;

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/pv <número>";
    private static final String ERR_INVALID_NUM = "&#FF5555[!] Error: Debes ingresar un número válido.";
    private static final String ERR_NO_PERM = "&#FF5555[!] Acceso denegado a la mochila compartimento #%num%.";

    public ComandoPV(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(NexoColor.parse(ERR_USAGE));
            return true;
        }

        try {
            int vaultNumber = Integer.parseInt(args[0]);

            if (!player.hasPermission("nexo.pv." + vaultNumber) && !player.hasPermission("nexo.pv.*")) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM.replace("%num%", String.valueOf(vaultNumber))));
                return true;
            }

            Inventory vault = plugin.getMochilaManager().obtenerMochila(player, vaultNumber);
            player.openInventory(vault);

        } catch (NumberFormatException e) {
            player.sendMessage(NexoColor.parse(ERR_INVALID_NUM));
        }

        return true;
    }
}