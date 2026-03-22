package me.nexo.war.core;

import java.math.BigDecimal;
import java.util.UUID;

public record WarContract(
        UUID warId,
        UUID clanAtacante,
        UUID clanDefensor,
        BigDecimal apuestaMonedas,
        long startTime,
        WarStatus status,
        int killsAtacante,
        int killsDefensor
) {
    public enum WarStatus {
        GRACE_PERIOD, // Tiempo de preparación (5 min)
        ACTIVE,       // En combate a muerte
        FINISHED      // Guerra terminada
    }
}