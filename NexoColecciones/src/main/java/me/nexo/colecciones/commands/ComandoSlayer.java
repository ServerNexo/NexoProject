package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import me.nexo.core.utils.NexoColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoSlayer implements CommandExecutor {
    private final NexoColecciones plugin;

    // 🎨 PALETA HEX - CONSTANTES
    private static final String ERR_NOT_PLAYER = "&#ff4b2bError: El terminal requiere un operario humano.";
    private static final String MSG_CANCELLED = "&#fbd72bProtocolo de cacería abortado exitosamente.";
    private static final String ERR_NO_ACTIVE = "&#ff4b2bRegistro limpio. No tienes ninguna cacería activa en este momento.";

    public ComandoSlayer(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        // ❌ Cancelar misión
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            ActiveSlayer activo = plugin.getSlayerManager().getActiveSlayer(player.getUniqueId());
            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                plugin.getSlayerManager().removeActiveSlayer(player.getUniqueId());
                player.sendMessage(NexoColor.parse(MSG_CANCELLED));
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_ACTIVE));
            }
            return true;
        }

        // ⚔️ Iniciar directo
        if (args.length == 1 && !args[0].equalsIgnoreCase("cancel")) {
            plugin.getSlayerManager().iniciarSlayer(player, args[0]);
            return true;
        }

        // 🖼️ Abrir Menú
        SlayerMenu.abrirMenu(player, plugin.getSlayerManager());
        return true;
    }
}