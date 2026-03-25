package me.nexo.core.crossplay;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public class NexoDisplayFixer {

    /**
     * 🛡️ Interaction Bridge para Modelos 3D:
     * Genera una Hitbox (caja de colisión) invisible exactamente donde está el modelo 3D.
     * Esto le da a los jugadores de Bedrock una masa física sólida que pueden tocar con el dedo.
     * * @param display La entidad ItemDisplay (Ej: El modelo del Minion o Boss).
     * @param width Ancho de la hitbox (Recomendado: 1.2f).
     * @param height Alto de la hitbox (Recomendado: 1.5f).
     * @param makePassenger Si es true, la hitbox viajará pegada al modelo si este se mueve.
     * @return La entidad Interaction (¡Guárdala para poder eliminarla cuando borres el ItemDisplay!).
     */
    public static Interaction spawnBedrockHitbox(ItemDisplay display, float width, float height, boolean makePassenger) {
        Location loc = display.getLocation();
        Interaction hitbox = loc.getWorld().spawn(loc, Interaction.class, interaction -> {
            interaction.setInteractionWidth(width);
            interaction.setInteractionHeight(height);
            interaction.setResponsive(true); // Fuerza al cliente a reconocer el clic
        });

        if (makePassenger) {
            display.addPassenger(hitbox);
        }

        return hitbox;
    }

    /**
     * 📱 Anti-Jitter para Hologramas:
     * El cliente de Bedrock sufre espasmos al intentar interpolar el giro (Billboard) del texto.
     * Esto desactiva el suavizado, haciendo que el holograma se vea fijo y nítido en móviles.
     *
     * @param hologram El holograma (TextDisplay) recién spawneado.
     */
    public static void applyAntiJitter(TextDisplay hologram) {
        hologram.setBillboard(TextDisplay.Billboard.CENTER);

        // Cero milisegundos de interpolación erradica el temblor en móviles
        hologram.setInterpolationDuration(0);
        hologram.setInterpolationDelay(0);

        // Optimizamos las sombras que causan caídas de FPS en Android/iOS
        hologram.setShadowRadius(0f);
        hologram.setShadowStrength(0f);
    }
}