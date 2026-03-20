package me.nexo.colecciones.commands;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.ColeccionesMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoColecciones implements CommandExecutor {
    private final NexoColecciones plugin;

    public ComandoColecciones(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            // Llama a nuestro nuevo menú
            ColeccionesMenu.abrirMenu(player, plugin.getCollectionManager());
            return true;
        }
        return false;
    }
}