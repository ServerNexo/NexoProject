package me.nexo.colecciones.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionItem;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ColeccionesExpansion extends PlaceholderExpansion {

    private final NexoColecciones plugin;
    private final CollectionManager manager;

    public ColeccionesExpansion(NexoColecciones plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCollectionManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexocolecciones"; // El inicio de tu variable: %nexocolecciones_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Mantiene la expansión activa al recargar PAPI
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "0";

        CollectionProfile profile = manager.getProfile(player.getUniqueId());
        if (profile == null) return "0";

        // 🌟 VARIABLE 1: %nexocolecciones_progress_ITEM%
        if (params.startsWith("progress_")) {
            String itemId = params.replace("progress_", "").toLowerCase(); // Usamos minúsculas para coincidir con el motor
            return String.valueOf(profile.getProgress(itemId));
        }

        // 🌟 VARIABLE 2: %nexocolecciones_level_ITEM%
        if (params.startsWith("level_")) {
            String itemId = params.replace("level_", "").toLowerCase();

            // Buscamos el ítem en la memoria del Cerebro
            CollectionItem item = manager.getItemGlobal(itemId);
            if (item == null) return "0"; // Si no existe, es nivel 0

            int progreso = profile.getProgress(itemId);

            // 🌟 CORRECCIÓN: Le pasamos el 'item' al calculador para que lea sus Tiers
            return String.valueOf(manager.calcularNivel(item, progreso));
        }

        return null; // Si escriben mal la variable
    }
}