package me.nexo.items.guardarropa;

import me.nexo.core.utils.NexoColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoWardrobe implements CommandExecutor {

    private final GuardarropaListener listener;

    private static final String ERR_NOT_PLAYER = "&#8b0000[!] El terminal requiere un operario humano.";
    private static final String BC_DIVIDER = "&#1c0f2a========================================";
    private static final String MSG_TITLE = "&#ff00ff<bold>👕 MÓDULO DE GUARDARROPA</bold>";
    private static final String MSG_HELP_OPEN = "&#ff00ff/wardrobe &#1c0f2a- Abre el panel de tu Guardarropa.";

    public ComandoWardrobe(GuardarropaListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length == 0) {
            listener.abrirMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            player.sendMessage(NexoColor.parse(MSG_TITLE));
            player.sendMessage(NexoColor.parse(MSG_HELP_OPEN));
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            return true;
        }

        return true;
    }
}