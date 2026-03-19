package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoNexo implements CommandExecutor {

    private final NexoCore plugin;

    public ComandoNexo(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("nexo.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 3) {
            Player objetivo = Bukkit.getPlayer(args[1]);
            if (objetivo == null) {
                sender.sendMessage("§cEl jugador no está conectado.");
                return true;
            }

            try {
                int cantidad = Integer.parseInt(args[2]);

                // 🟢 ARQUITECTURA LIMPIA: Obtenemos los datos del jugador desde la API local
                NexoUser user = NexoAPI.getInstance().getUserLocal(objetivo.getUniqueId());
                if (user == null) {
                    sender.sendMessage("§cLos datos del jugador aún están cargando...");
                    return true;
                }

                // 1. Comando de Nexo XP Global
                if (args[0].equalsIgnoreCase("darxp")) {
                    int nivelActual = user.getNexoNivel();
                    int xpActual = user.getNexoXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;
                        objetivo.sendTitle("§e§l¡NEXO NIVEL " + nivelActual + "!", "§fHas ascendido", 10, 70, 20);
                    }

                    user.setNexoNivel(nivelActual);
                    user.setNexoXp(xpActual);

                    if (sender instanceof Player) sender.sendMessage("§aHas dado " + cantidad + " Nexo XP a " + objetivo.getName());
                }

                // 2. Comando de Combate XP
                else if (args[0].equalsIgnoreCase("darcombatexp")) {
                    int nivelActual = user.getCombateNivel();
                    int xpActual = user.getCombateXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;
                        objetivo.sendTitle("§c§l¡COMBATE NIVEL " + nivelActual + "!", "§7Tus instintos mejoran...", 10, 70, 20);
                    }

                    user.setCombateNivel(nivelActual);
                    user.setCombateXp(xpActual);

                    objetivo.sendMessage("§c⚔ +" + cantidad + " XP §8(§7" + xpActual + "/" + (nivelActual * 100) + "§8)");
                    if (sender instanceof Player) sender.sendMessage("§cHas dado " + cantidad + " Combate XP a " + objetivo.getName());
                }

            } catch (NumberFormatException e) {
                sender.sendMessage("§cLa cantidad debe ser un número válido.");
            }
            return true;
        }

        sender.sendMessage("§cUso correcto: /nexocore <darxp|darcombatexp> <jugador> <cantidad>");
        return true;
    }
}