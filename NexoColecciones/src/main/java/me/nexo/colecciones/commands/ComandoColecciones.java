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

        // 💻 Si el comando se ejecuta desde la CONSOLA
        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.getColeccionesConfig().recargarConfig();
                plugin.getCollectionManager().cargarDesdeConfig();
                sender.sendMessage("§a✅ [NexoColecciones] Configuración recargada con éxito desde la consola.");
            }
            return true;
        }

        // 🌟 NUEVO: Comando Reload (/colecciones reload) - SOLO ADMINS IN-GAME
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexocolecciones.admin")) {
                player.sendMessage("§c¡No tienes permiso para hacer esto!");
                return true;
            }

            // 1. Forzamos la lectura del disco con tu método existente
            plugin.getColeccionesConfig().recargarConfig();

            // 2. Le decimos al Cerebro que limpie la RAM y cargue los nuevos datos
            plugin.getCollectionManager().cargarDesdeConfig();

            plugin.getSlayerManager().cargarSlayers();

            player.sendMessage("§a✅ ¡Configuración de Colecciones recargada con éxito!");
            return true;
        }

        // 🏆 Comando Top (/colecciones top ZOMBIE)
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            plugin.getCollectionManager().calcularTopAsync(player, args[1]);
            return true;
        }

        // ⚔️ Comando Slayer (/colecciones slayer <ID>)
        if (args.length == 2 && args[0].equalsIgnoreCase("slayer")) {
            plugin.getSlayerManager().iniciarSlayer(player, args[1]);
            return true;
        }


        // 🎒 Si solo escribe /colecciones, abrimos el Menú Principal
        ColeccionesMenu.abrirMenuPrincipal(player);
        return true;
    }
}