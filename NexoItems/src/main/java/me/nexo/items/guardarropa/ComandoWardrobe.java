package me.nexo.items.guardarropa;

import me.nexo.core.utils.NexoColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoWardrobe implements CommandExecutor {

    // 🌟 Restauramos la conexión con el Listener
    private final GuardarropaListener listener;

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String BC_DIVIDER = "&#555555========================================";
    private static final String MSG_TITLE = "&#FFAA00<bold>👕 MÓDULO DE GUARDARROPA</bold>";
    private static final String MSG_HELP_OPEN = "&#FFAA00/wardrobe &#AAAAAA- Abre el panel de tu Guardarropa.";

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
            // 🌟 Llamamos al método directamente, sin instanciar Inventory
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