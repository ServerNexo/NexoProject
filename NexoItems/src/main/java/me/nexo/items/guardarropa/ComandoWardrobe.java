package me.nexo.items.guardarropa;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoWardrobe implements CommandExecutor {

    private final NexoItems plugin;
    private final GuardarropaListener listener;

    public ComandoWardrobe(NexoItems plugin, GuardarropaListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.guardarropa.no-jugador"));
            return true;
        }

        if (args.length == 0) {
            listener.abrirMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.guardarropa.ayuda.divisor"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.guardarropa.ayuda.titulo"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.guardarropa.ayuda.abrir"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.guardarropa.ayuda.divisor"));
            return true;
        }

        return true;
    }
}