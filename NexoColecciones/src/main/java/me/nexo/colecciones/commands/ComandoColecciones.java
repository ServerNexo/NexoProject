package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.ColeccionesMenu;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoColecciones implements CommandExecutor {
    private final NexoColecciones plugin;

    public ComandoColecciones(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        return NexoCore.getPlugin(NexoCore.class).getConfigManager().getMessage("colecciones_messages.yml", path);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.getColeccionesConfig().recargarConfig();
                plugin.getCollectionManager().cargarDesdeConfig();
                plugin.getSlayerManager().cargarSlayers();
                CrossplayUtils.sendMessage(null, getMessage("comandos.colecciones.recarga-exito"));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexocolecciones.admin")) {
                CrossplayUtils.sendMessage(player, getMessage("comandos.colecciones.sin-permiso"));
                return true;
            }

            plugin.getColeccionesConfig().recargarConfig();
            plugin.getCollectionManager().cargarDesdeConfig();
            plugin.getSlayerManager().cargarSlayers();

            CrossplayUtils.sendMessage(player, getMessage("comandos.colecciones.recarga-exito"));
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