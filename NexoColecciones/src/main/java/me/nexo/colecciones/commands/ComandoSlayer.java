package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoSlayer implements CommandExecutor {
    private final NexoColecciones plugin;

    public ComandoSlayer(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        return NexoCore.getPlugin(NexoCore.class).getConfigManager().getMessage("colecciones_messages.yml", path);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, getMessage("comandos.slayer.no-jugador"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            ActiveSlayer activo = plugin.getSlayerManager().getActiveSlayer(player.getUniqueId());
            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                plugin.getSlayerManager().removeActiveSlayer(player.getUniqueId());
                CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.cancelado"));
            } else {
                CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.no-activo"));
            }
            return true;
        }

        if (args.length == 1 && !args[0].equalsIgnoreCase("cancel")) {
            plugin.getSlayerManager().iniciarSlayer(player, args[0]);
            return true;
        }

        // 🌟 CORRECCIÓN APLICADA: Ahora instanciamos y abrimos el NexoMenu correctamente
        new SlayerMenu(player, plugin).open();
        return true;
    }
}