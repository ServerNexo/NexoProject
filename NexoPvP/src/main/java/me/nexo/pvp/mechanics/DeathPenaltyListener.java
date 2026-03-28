package me.nexo.pvp.mechanics;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.concurrent.CompletableFuture;

public class DeathPenaltyListener implements Listener {

    private final NexoCore core;

    public DeathPenaltyListener() {
        // Obtenemos la instancia del cerebro para leer el caché sin lag
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) return; // 🛡️ Salvaguarda Anti-Lag: Si no está en caché, no hacemos nada.

        // 🌟 LECTURA DESDE CACHÉ (Zero-Main-Thread SQL)
        boolean hasVoidBlessing = user.hasActiveBlessing("VOID_BLESSING");

        if (hasVoidBlessing) {
            // 🛡️ PROTECCIÓN DIVINA: Mantiene inventario y experiencia.
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Mensajes usando el Protocolo Cromático (Vivid Void)
            player.sendMessage(NexoColor.parse("&#ff00ff[⚡] La Bendición del Vacío ha protegido tu alma y tus pertenencias."));

            // Consumimos la bendición asíncronamente para mantener los 20 TPS
            CompletableFuture.runAsync(() -> {
                user.removeBlessing("VOID_BLESSING");
                core.getDatabaseManager().saveUserBlessings(user);
            });

        } else {
            // 🩸 HARDCORE PENALTY (Filosofía Tibia)
            // Pierde un 10% de su nivel actual. Los ítems caen al suelo por comportamiento Vanilla.
            int currentLevel = player.getLevel();
            int penaltyLevel = (int) (currentLevel * 0.10);

            event.setNewLevel(Math.max(0, currentLevel - penaltyLevel));

            player.sendMessage(NexoColor.parse("&#8b0000[☠] Has perecido. Tu cuerpo ha sido saqueado y has perdido energía vital."));
            player.sendMessage(NexoColor.parse("&#00f5ff[💡] Consejo: Adquiere una Bendición en el Templo para proteger tu equipo en el futuro."));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }
}