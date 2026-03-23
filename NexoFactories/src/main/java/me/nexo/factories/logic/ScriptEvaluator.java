package me.nexo.factories.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.core.ProtectionStone;

public class ScriptEvaluator {

    /**
     * Evalúa si una máquina DEBE encenderse o apagarse basándose en su Script personalizado.
     * Complejidad O(1) - Evaluado asíncronamente en Virtual Threads.
     * * @return true si la máquina puede operar, false si el script ordena detenerla.
     */
    public boolean shouldRun(ActiveFactory factory, ProtectionStone stone, String jsonScript) {
        // Si la máquina no tiene un script instalado, opera normalmente
        if (jsonScript == null || jsonScript.isEmpty() || jsonScript.equals("NONE")) {
            return true;
        }

        try {
            JsonObject logic = JsonParser.parseString(jsonScript).getAsJsonObject();

            // 🌟 ESTRUCTURA DEL JSON LOGIC CARD:
            // {"condition": "ENERGY_>_50", "action": "START_MACHINE"}
            // Más adelante esto será un árbol complejo de decisiones (AND, OR)

            if (!logic.has("condition")) return true;
            String condition = logic.get("condition").getAsString();

            // REGLA 1: Prioridad de Escudo (Ahorrar energía si la base está bajo ataque o baja batería)
            if (condition.startsWith("ENERGY_>_")) {
                double requiredEnergy = Double.parseDouble(condition.split("_>_")[1]);
                return stone.getCurrentEnergy() > requiredEnergy;
            }

            // REGLA 2: Límite de Almacenamiento (No producir si ya hay mucho)
            if (condition.startsWith("STORAGE_<_")) {
                int maxStorage = Integer.parseInt(condition.split("_<_")[1]);
                return factory.getStoredOutput() < maxStorage;
            }

            return true; // Si no hay reglas que impidan el funcionamiento, continuar

        } catch (Exception e) {
            // Si el jugador rompió el script (Hack/Exploit), apagamos la máquina por seguridad
            return false;
        }
    }
}