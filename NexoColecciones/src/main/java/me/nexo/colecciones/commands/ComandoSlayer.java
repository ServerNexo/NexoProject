package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoSlayer implements CommandExecutor {
    private final NexoColecciones plugin;

    public ComandoSlayer(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        // ❌ Cancelar misión (/slayer cancel)
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            ActiveSlayer activo = plugin.getSlayerManager().getActiveSlayer(player.getUniqueId());
            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                plugin.getSlayerManager().removeActiveSlayer(player.getUniqueId());
                player.sendMessage("§cHas abandonado tu cacería actual.");
            } else {
                player.sendMessage("§cNo tienes ninguna cacería activa.");
            }
            return true;
        }

        // ⚔️ Iniciar una misión directo por comando (/slayer ZOMBIE_TIER_1)
        // Útil si en el futuro quieres poner NPCs que ejecuten el comando
        if (args.length == 1 && !args[0].equalsIgnoreCase("cancel")) {
            plugin.getSlayerManager().iniciarSlayer(player, args[0]);
            return true;
        }

        // 🖼️ Si solo escribe /slayer, abrimos el menú gráfico
        SlayerMenu.abrirMenu(player, plugin.getSlayerManager());
        return true;
    }
}