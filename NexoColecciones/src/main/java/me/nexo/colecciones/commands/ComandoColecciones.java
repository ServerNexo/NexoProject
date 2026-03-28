package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.ColeccionesMenu;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoColecciones implements CommandExecutor {
    private final NexoColecciones plugin;

    private static final String MSG_RELOAD_SUCCESS = "&#00f5ff[✓] Sistemas de Colecciones y Slayers sincronizados y en línea.";
    private static final String ERR_NO_PERM = "&#8b0000Acceso Denegado. Se requiere autorización de administrador.";

    public ComandoColecciones(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.getColeccionesConfig().recargarConfig();
                plugin.getCollectionManager().cargarDesdeConfig();
                plugin.getSlayerManager().cargarSlayers();
                sender.sendMessage(CrossplayUtils.parseCrossplay(null, MSG_RELOAD_SUCCESS));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexocolecciones.admin")) {
                CrossplayUtils.sendMessage(player, ERR_NO_PERM);
                return true;
            }

            plugin.getColeccionesConfig().recargarConfig();
            plugin.getCollectionManager().cargarDesdeConfig();
            plugin.getSlayerManager().cargarSlayers();

            CrossplayUtils.sendMessage(player, MSG_RELOAD_SUCCESS);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            plugin.getCollectionManager().calcularTopAsync(player, args[1]);
            return true;
        }

        new ColeccionesMenu(plugin, player, ColeccionesMenu.MenuType.MAIN).openMain();
        return true;
    }
}