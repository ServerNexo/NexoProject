package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class ComandoNexo implements CommandExecutor {

    private final NexoCore plugin;

    // 🎨 PALETA HEX - CONSTANTES
    private static final String ERR_NO_PERM = "&#ff4b2b[!] Acceso denegado: Autorización de Administrador requerida.";
    private static final String ERR_OFFLINE = "&#ff4b2b[!] Error: El operario objetivo no está en línea.";
    private static final String ERR_LOADING = "&#fbd72b[!] Sincronizando datos con la red. Intente de nuevo en unos segundos...";
    private static final String ERR_FORMAT = "&#ff4b2b[!] Error de formato: La cantidad debe ser numérica.";
    private static final String MSG_USAGE = "&#434343Uso del sistema: &#fbd72b/nexocore <darxp|darcombatexp> <operario> <cantidad>";

    public ComandoNexo(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("nexo.admin")) {
            sender.sendMessage(NexoColor.parse(ERR_NO_PERM));
            return true;
        }

        if (args.length == 3) {
            Player objetivo = Bukkit.getPlayer(args[1]);
            if (objetivo == null) {
                sender.sendMessage(NexoColor.parse(ERR_OFFLINE));
                return true;
            }

            try {
                int cantidad = Integer.parseInt(args[2]);
                NexoUser user = NexoAPI.getInstance().getUserLocal(objetivo.getUniqueId());

                if (user == null) {
                    sender.sendMessage(NexoColor.parse(ERR_LOADING));
                    return true;
                }

                Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));

                // 1. Comando de Nexo XP Global
                if (args[0].equalsIgnoreCase("darxp")) {
                    int nivelActual = user.getNexoNivel();
                    int xpActual = user.getNexoXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;

                        // 🌟 TÍTULO NATIVO MODERNIZADO
                        Title title = Title.title(
                                NexoColor.parse("&#fbd72b<bold>¡NEXO NIVEL " + nivelActual + "!</bold>"),
                                NexoColor.parse("&#a8ff78Has ascendido en la jerarquía del servidor"),
                                times
                        );
                        objetivo.showTitle(title);
                    }

                    user.setNexoNivel(nivelActual);
                    user.setNexoXp(xpActual);

                    if (sender instanceof Player) sender.sendMessage(NexoColor.parse("&#a8ff78[✓] Transferencia de " + cantidad + " Nexo XP a " + objetivo.getName() + " completada."));
                }

                // 2. Comando de Combate XP
                else if (args[0].equalsIgnoreCase("darcombatexp")) {
                    int nivelActual = user.getCombateNivel();
                    int xpActual = user.getCombateXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;

                        // 🌟 TÍTULO NATIVO MODERNIZADO
                        Title title = Title.title(
                                NexoColor.parse("&#ff4b2b<bold>¡COMBATE NIVEL " + nivelActual + "!</bold>"),
                                NexoColor.parse("&#434343Tus instintos bélicos se agudizan..."),
                                times
                        );
                        objetivo.showTitle(title);
                    }

                    user.setCombateNivel(nivelActual);
                    user.setCombateXp(xpActual);

                    objetivo.sendMessage(NexoColor.parse("&#ff4b2b⚔ +" + cantidad + " XP de Combate &#434343(" + xpActual + "/" + (nivelActual * 100) + ")"));
                    if (sender instanceof Player) sender.sendMessage(NexoColor.parse("&#ff4b2b[✓] Transferencia de " + cantidad + " Combate XP a " + objetivo.getName() + " completada."));
                }

            } catch (NumberFormatException e) {
                sender.sendMessage(NexoColor.parse(ERR_FORMAT));
            }
            return true;
        }

        sender.sendMessage(NexoColor.parse(MSG_USAGE));
        return true;
    }
}