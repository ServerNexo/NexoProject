package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando Principal (Arquitectura Enterprise / Lamp Framework)
 * Cero 'args.length', cero 'CommandExecutor', inyección limpia.
 */
@Command({"nexocore", "nexo"})
@CommandPermission("nexo.admin")
public class ComandoNexo {

    // 💉 PILAR 3: Inyección de Dependencias
    private final UserManager userManager;
    private final ConfigManager configManager;

    @Inject
    public ComandoNexo(UserManager userManager, ConfigManager configManager) {
        this.userManager = userManager;
        this.configManager = configManager;
    }

    @Subcommand("darxp")
    public void darXp(CommandSender sender, Player objetivo, int cantidad) {
        // 💡 PILAR 1: Lamp ya valida si "cantidad" es un número y si el jugador está online.
        NexoUser user = userManager.getUserOrNull(objetivo.getUniqueId());

        if (user == null) {
            enviarMensaje(sender, configManager.getMessage("comandos.nexocore.errores.cargando"));
            return;
        }

        int nivelActual = user.getNexoNivel();
        int xpActual = user.getNexoXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;
            CrossplayUtils.sendTitle(objetivo,
                    configManager.getMessage("comandos.nexocore.subida-nivel.nexo.titulo").replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessage("comandos.nexocore.subida-nivel.nexo.subtitulo")
            );
        }

        user.setNexoNivel(nivelActual);
        user.setNexoXp(xpActual);

        enviarMensaje(sender, configManager.getMessage("comandos.nexocore.exito.dar-xp")
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    @Subcommand("darcombatexp")
    public void darCombateXp(CommandSender sender, Player objetivo, int cantidad) {
        NexoUser user = userManager.getUserOrNull(objetivo.getUniqueId());

        if (user == null) {
            enviarMensaje(sender, configManager.getMessage("comandos.nexocore.errores.cargando"));
            return;
        }

        int nivelActual = user.getCombateNivel();
        int xpActual = user.getCombateXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;
            CrossplayUtils.sendTitle(objetivo,
                    configManager.getMessage("comandos.nexocore.subida-nivel.combate.titulo").replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessage("comandos.nexocore.subida-nivel.combate.subtitulo")
            );
        }

        user.setCombateNivel(nivelActual);
        user.setCombateXp(xpActual);

        CrossplayUtils.sendMessage(objetivo, configManager.getMessage("comandos.nexocore.feedback.recibir-combate-xp")
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%xp%", String.valueOf(xpActual))
                .replace("%xpreq%", String.valueOf(nivelActual * 100)));

        enviarMensaje(sender, configManager.getMessage("comandos.nexocore.exito.dar-combate-xp")
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    // 📱 PILAR 6: Conciencia Cross-Play y soporte para la Consola usando Java 21 Pattern Matching
    private void enviarMensaje(CommandSender sender, String mensaje) {
        if (sender instanceof Player player) {
            CrossplayUtils.sendMessage(player, mensaje);
        } else {
            sender.sendMessage(mensaje);
        }
    }
}