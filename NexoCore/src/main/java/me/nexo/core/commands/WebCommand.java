package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.user.UserRepository;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ Nexo Network - Comando Web (Arquitectura Enterprise / Lamp Framework)
 * Encriptación y guardado 100% asíncrono y reactivo.
 */
@Command("web")
public class WebCommand {

    // 💉 PILAR 3: Inyectamos el DAO, ya no necesitamos el NexoCore ni el DatabaseManager
    private final UserRepository userRepository;

    @Inject
    public WebCommand(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 💡 PILAR 1: Lamp maneja automáticamente que solo jugadores usen esto
    // y que obligatoriamente escriban un texto (password).
    @Subcommand("register")
    public void register(Player player, String password) {

        // 🚀 PILAR 4: Programación Reactiva Asíncrona (CompletableFuture)
        CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Encriptamos en un hilo secundario para no congelar el servidor
                return hashPassword(password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(hashedPassword ->
                // 2. Encadenamos el resultado al DAO de la base de datos
                userRepository.updateWebPassword(player.getUniqueId(), hashedPassword)
        ).thenAccept(success -> {
            // 3. Respondemos al jugador según el resultado de Supabase
            if (success) {
                player.sendMessage("§d§l[Nexo Web] §a¡Tu Clave del Vacío ha sido registrada con éxito!");
                player.sendMessage("§7Ya puedes iniciar sesión en el panel web.");
            } else {
                player.sendMessage("§c❌ Error: No se encontró tu perfil en la base de datos. ¡Vuelve a entrar al servidor!");
            }
        }).exceptionally(ex -> {
            // 🛡️ Manejo de errores a prueba de balas
            player.sendMessage("§c❌ Ocurrió un error crítico de seguridad al registrar tu clave.");
            ex.printStackTrace();
            return null;
        });
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