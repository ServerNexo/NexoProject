package me.nexo.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.nexo.core.NexoCore;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WebCommand implements CommandExecutor {

    private final NexoCore plugin;

    public WebCommand(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("register")) {
            player.sendMessage("§c❌ Uso correcto: /web register <tu_contraseña>");
            return true;
        }

        String rawPassword = args[1];

        // 🛡️ Nunca bloqueamos el hilo principal (Main Thread) al hacer consultas SQL
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Encriptar la contraseña (Hashing SHA-256)
                String hashedPassword = hashPassword(rawPassword);

                // 2. Aquí llamas a tu método de base de datos para hacer el UPDATE
                // Ejemplo ficticio: plugin.getDatabase().updateWebPassword(player.getUniqueId(), hashedPassword);

                // (Debes ejecutar un SQL similar a este):
                // UPDATE jugadores SET web_password = 'el_hash' WHERE uuid = 'uuid_del_jugador';

                player.sendMessage("§d§l[Nexo Web] §a¡Tu Clave del Vacío ha sido registrada con éxito!");
                player.sendMessage("§7Ya puedes iniciar sesión en el panel web.");

            } catch (Exception e) {
                player.sendMessage("§c❌ Ocurrió un error al registrar tu clave. Reporta esto a un administrador.");
                e.printStackTrace();
            }
        });

        return true;
    }

    // 🔒 Método interno para encriptar la contraseña
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        // Convertir bytes a Hexadecimal
        StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}