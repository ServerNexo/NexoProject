package me.nexo.colecciones.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
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
            String itemId = params.replace("progress_", "").toUpperCase();
            return String.valueOf(profile.getProgress(itemId));
        }

        // 🌟 VARIABLE 2: %nexocolecciones_level_ITEM%
        if (params.startsWith("level_")) {
            String itemId = params.replace("level_", "").toUpperCase();
            int progreso = profile.getProgress(itemId);
            return String.valueOf(manager.calcularNivel(progreso));
        }

        return null; // Si escriben mal la variable
    }
}