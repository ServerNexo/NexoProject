package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoSlayer implements CommandExecutor {
    private final NexoColecciones plugin;

    private static final String ERR_NOT_PLAYER = "&#8b0000Error: El terminal requiere un operario humano.";
    private static final String MSG_CANCELLED = "&#00f5ffProtocolo de cacería abortado exitosamente.";
    private static final String ERR_NO_ACTIVE = "&#8b0000Registro limpio. No tienes ninguna cacería activa en este momento.";

    public ComandoSlayer(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, ERR_NOT_PLAYER);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            ActiveSlayer activo = plugin.getSlayerManager().getActiveSlayer(player.getUniqueId());
            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                plugin.getSlayerManager().removeActiveSlayer(player.getUniqueId());
                CrossplayUtils.sendMessage(player, MSG_CANCELLED);
            } else {
                CrossplayUtils.sendMessage(player, ERR_NO_ACTIVE);
            }
            return true;
        }

        if (args.length == 1 && !args[0].equalsIgnoreCase("cancel")) {
            plugin.getSlayerManager().iniciarSlayer(player, args[0]);
            return true;
        }

        SlayerMenu.abrirMenu(player, plugin.getSlayerManager());
        return true;
    }
}