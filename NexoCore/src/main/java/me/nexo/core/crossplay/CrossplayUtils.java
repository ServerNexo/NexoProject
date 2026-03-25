package me.nexo.core.crossplay;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class CrossplayUtils {

    /**
     * 🛡️ Filtro de Texto Cross-Play:
     * Envía un mensaje optimizado. Si el receptor es de móvil/consola (Bedrock),
     * elimina los caracteres Unicode (Iconos custom de Java en el rango PUA) para evitar "cuadros rotos".
     */
    public static void sendMessage(Player player, String rawMessage) {
        player.sendMessage(parseCrossplay(player, rawMessage));
    }

    /**
     * 🛡️ Action Bar Cross-Play
     */
    public static void sendActionBar(Player player, String rawMessage) {
        player.sendActionBar(parseCrossplay(player, rawMessage));
    }

    /**
     * Motor interno de traducción de componentes.
     */
    public static Component parseCrossplay(Player player, String rawMessage) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            // Elimina mediante RegEx cualquier caracter en el rango E000-F8FF (Uso Privado / Custom Fonts)
            rawMessage = rawMessage.replaceAll("[\\uE000-\\uF8FF]", "");
        }
        return NexoColor.parse(rawMessage);
    }

    /**
     * 📱 GUI Scaling Dinámico:
     * Ajusta el tamaño de los inventarios (GUIs).
     * En Java, un menú de 54 slots se ve bien. En celulares táctiles, bloquea la visión del inventario inferior.
     */
    public static int getOptimizedMenuSize(Player player, int targetSize) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            // Si el menú original es masivo (54 o 45 slots), lo compactamos a 36 para Bedrock
            if (targetSize > 36) {
                return 36;
            }
        }
        return targetSize; // Retorna el tamaño original para los de PC
    }
}