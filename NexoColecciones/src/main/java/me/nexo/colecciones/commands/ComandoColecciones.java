package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.ColeccionesMenu;
import me.nexo.core.utils.NexoColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoColecciones implements CommandExecutor {
    private final NexoColecciones plugin;

    // 🎨 PALETA HEX - CONSTANTES
    private static final String MSG_RELOAD_SUCCESS = "&#a8ff78[✓] Sistemas de Colecciones y Slayers sincronizados y en línea.";
    private static final String ERR_NO_PERM = "&#ff4b2bAcceso Denegado. Se requiere autorización de administrador.";

    public ComandoColecciones(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 💻 CONSOLA
        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.getColeccionesConfig().recargarConfig();
                plugin.getCollectionManager().cargarDesdeConfig();
                plugin.getSlayerManager().cargarSlayers();
                sender.sendMessage(NexoColor.parse(MSG_RELOAD_SUCCESS));
            }
            return true;
        }

        // 🌟 RELOAD (SOLO ADMINS IN-GAME)
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexocolecciones.admin")) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM));
                return true;
            }

            plugin.getColeccionesConfig().recargarConfig();
            plugin.getCollectionManager().cargarDesdeConfig();
            plugin.getSlayerManager().cargarSlayers();

            player.sendMessage(NexoColor.parse(MSG_RELOAD_SUCCESS));
            return true;
        }

        // 🏆 TOP
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            plugin.getCollectionManager().calcularTopAsync(player, args[1]);
            return true;
        }

        // 🎒 ABRIR MENÚ
        ColeccionesMenu.abrirMenuPrincipal(player);
        return true;
    }
}