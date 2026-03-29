package me.nexo.core;

import me.nexo.core.crossplay.CrossplayUtils;
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
            enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.errores.sin-permiso"));
            return true;
        }

        if (args.length == 3) {
            Player objetivo = Bukkit.getPlayer(args[1]);
            if (objetivo == null) {
                enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.errores.offline"));
                return true;
            }

            try {
                int cantidad = Integer.parseInt(args[2]);

                // 🛡️ CORRECCIÓN 1: Usamos la instancia inyectada del plugin (Seguridad absoluta contra NullPointers)
                NexoUser user = plugin.getUserManager().getUserOrNull(objetivo.getUniqueId());

                if (user == null) {
                    enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.errores.cargando"));
                    return true;
                }

                if (args[0].equalsIgnoreCase("darxp")) {
                    int nivelActual = user.getNexoNivel();
                    int xpActual = user.getNexoXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;
                        CrossplayUtils.sendTitle(objetivo,
                                plugin.getConfigManager().getMessage("comandos.nexocore.subida-nivel.nexo.titulo").replace("%level%", String.valueOf(nivelActual)),
                                plugin.getConfigManager().getMessage("comandos.nexocore.subida-nivel.nexo.subtitulo")
                        );
                    }

                    user.setNexoNivel(nivelActual);
                    user.setNexoXp(xpActual);

                    enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.exito.dar-xp")
                            .replace("%amount%", String.valueOf(cantidad))
                            .replace("%target%", objetivo.getName()));

                } else if (args[0].equalsIgnoreCase("darcombatexp")) {
                    int nivelActual = user.getCombateNivel();
                    int xpActual = user.getCombateXp() + cantidad;

                    while (xpActual >= (nivelActual * 100)) {
                        xpActual -= (nivelActual * 100);
                        nivelActual++;
                        CrossplayUtils.sendTitle(objetivo,
                                plugin.getConfigManager().getMessage("comandos.nexocore.subida-nivel.combate.titulo").replace("%level%", String.valueOf(nivelActual)),
                                plugin.getConfigManager().getMessage("comandos.nexocore.subida-nivel.combate.subtitulo")
                        );
                    }

                    user.setCombateNivel(nivelActual);
                    user.setCombateXp(xpActual);

                    CrossplayUtils.sendMessage(objetivo, plugin.getConfigManager().getMessage("comandos.nexocore.feedback.recibir-combate-xp")
                            .replace("%amount%", String.valueOf(cantidad))
                            .replace("%xp%", String.valueOf(xpActual))
                            .replace("%xpreq%", String.valueOf(nivelActual * 100)));

                    enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.exito.dar-combate-xp")
                            .replace("%amount%", String.valueOf(cantidad))
                            .replace("%target%", objetivo.getName()));
                }

            } catch (NumberFormatException e) {
                enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.errores.formato-invalido"));
            }
            return true;
        }

        enviarMensaje(sender, plugin.getConfigManager().getMessage("comandos.nexocore.uso"));
        return true;
    }

    // 🛡️ CORRECCIÓN 2: Método modular para soportar envíos desde la Consola sin crashear
    private void enviarMensaje(CommandSender sender, String mensaje) {
        if (sender instanceof Player) {
            CrossplayUtils.sendMessage((Player) sender, mensaje);
        } else {
            // Si es la consola quien envía el comando, se imprime sin usar utilidades gráficas
            sender.sendMessage(mensaje);
        }
    }
}