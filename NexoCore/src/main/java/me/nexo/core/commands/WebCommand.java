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
            player.sendMessage("§c❌ Uso correcto: /web register <tu_contraseña_secreta>");
            player.sendMessage("§7§o(Esta contraseña servirá para entrar a tu Panel Web)");
            return true;
        }

        String rawPassword = args[1];

        // Encriptamos asíncronamente para máxima seguridad y rendimiento
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Encriptar la contraseña a SHA-256
                String hashedPassword = hashPassword(rawPassword);

                // 2. Enviar la contraseña encriptada a tu DatabaseManager
                plugin.getDatabaseManager().actualizarClaveWeb(player, hashedPassword);

            } catch (Exception e) {
                player.sendMessage("§c❌ Error de seguridad al procesar tu clave.");
                e.printStackTrace();
            }
        });

        return true;
    }

    // 🔒 Encriptación SHA-256 (Nativa de Java, Cero Dependencias)
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

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