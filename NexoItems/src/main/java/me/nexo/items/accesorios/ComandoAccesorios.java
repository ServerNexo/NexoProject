package me.nexo.items.accesorios;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoAccesorios implements CommandExecutor {

    private final NexoItems plugin;

    // 🎨 PALETA HEX CORREGIDA
    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String BC_DIVIDER = "&#555555========================================";
    private static final String MSG_TITLE = "&#FFAA00<bold>💍 MÓDULO DE ACCESORIOS (BETA)</bold>";
    private static final String MSG_HELP_OPEN = "&#FFAA00/accesorios &#AAAAAA- Abre tu bolsa de accesorios.";
    private static final String MSG_HELP_GIVE = "&#FFAA00/accesorios give <jugador> <id> &#AAAAAA- Otorga un accesorio a un operario.";
    private static final String ERR_PERM = "&#FF5555[!] Acceso denegado al módulo administrativo.";
    private static final String ERR_USAGE_GIVE = "&#FF5555[!] Uso: &#FFAA00/accesorios give <jugador> <id>";
    private static final String ERR_OFFLINE = "&#FF5555[!] El operario no se encuentra en línea.";
    private static final String ERR_NOT_FOUND = "&#FF5555[!] Accesorio no registrado en la base de datos: &#FFAA00%id%";
    private static final String MSG_GIVE_SUCCESS = "&#55FF55[✓] Accesorio entregado con éxito.";
    private static final String MSG_RECEIVE = "&#00E5FF[NEXO] Has recibido un nuevo Accesorio de la corporación.";

    public ComandoAccesorios(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        // 🌟 CORRECCIÓN AQUÍ: Llamamos a abrirBolsa(), que hace la consulta asíncrona y abre la UI.
        if (args.length == 0) {
            plugin.getAccesoriosManager().abrirBolsa(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            player.sendMessage(NexoColor.parse(MSG_TITLE));
            player.sendMessage(NexoColor.parse(MSG_HELP_OPEN));
            if (player.hasPermission("nexo.admin")) {
                player.sendMessage(NexoColor.parse(MSG_HELP_GIVE));
            }
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("nexo.admin")) {
                player.sendMessage(NexoColor.parse(ERR_PERM));
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(NexoColor.parse(ERR_USAGE_GIVE));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(NexoColor.parse(ERR_OFFLINE));
                return true;
            }

            String accId = args[2].toLowerCase();
            // Ten en cuenta que si AccesoriosManager no tiene generarAccesorio, te dará error aquí.
            // Abajo te indico cómo arreglarlo por si acaso.
            org.bukkit.inventory.ItemStack item = plugin.getAccesoriosManager().generarAccesorio(accId);

            if (item == null) {
                player.sendMessage(NexoColor.parse(ERR_NOT_FOUND.replace("%id%", accId)));
                return true;
            }

            target.getInventory().addItem(item);
            target.sendMessage(NexoColor.parse(MSG_RECEIVE));
            player.sendMessage(NexoColor.parse(MSG_GIVE_SUCCESS));
            return true;
        }

        return true;
    }
}