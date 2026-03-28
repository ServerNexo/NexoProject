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

    // 🎨 PALETA VIVID VOID
    private static final String ERR_NOT_PLAYER = "&#8b0000[!] El terminal requiere un operario humano.";
    private static final String BC_DIVIDER = "&#1c0f2a========================================";
    private static final String MSG_TITLE = "&#ff00ff<bold>💍 MÓDULO DE ACCESORIOS (BETA)</bold>";
    private static final String MSG_HELP_OPEN = "&#ff00ff/accesorios &#1c0f2a- Abre tu bolsa de accesorios.";
    private static final String MSG_HELP_GIVE = "&#ff00ff/accesorios give <jugador> <id> &#1c0f2a- Otorga un accesorio a un operario.";
    private static final String ERR_PERM = "&#8b0000[!] Acceso denegado al módulo administrativo.";
    private static final String ERR_USAGE_GIVE = "&#8b0000[!] Uso: &#ff00ff/accesorios give <jugador> <id>";
    private static final String ERR_OFFLINE = "&#8b0000[!] El operario no se encuentra en línea.";
    private static final String ERR_NOT_FOUND = "&#8b0000[!] Accesorio no registrado en la base de datos: &#ff00ff%id%";
    private static final String MSG_GIVE_SUCCESS = "&#00f5ff[✓] Accesorio entregado con éxito.";
    private static final String MSG_RECEIVE = "&#00f5ff[NEXO] Has recibido un nuevo Accesorio de la corporación.";

    public ComandoAccesorios(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

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