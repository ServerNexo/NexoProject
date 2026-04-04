package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

/**
 * 🏛️ NexoPvP - Comando PvP (Arquitectura Enterprise)
 * Cero CommandExecutor, Cero Chequeos de Consola.
 */
public class ComandoPvP {

    // 💉 PILAR 3: Inyección de Dependencias Limpia
    private final PvPManager manager;

    @Inject
    public ComandoPvP(PvPManager manager) {
        this.manager = manager;
    }

    // 💡 PILAR 1: Framework Lamp
    @Command("pvp")
    public void togglePvP(Player player) {
        // Al pedir "Player" como parámetro, Lamp bloquea automáticamente a la consola
        // y envía un mensaje de error si no es un jugador.
        manager.togglePvP(player);
    }
}