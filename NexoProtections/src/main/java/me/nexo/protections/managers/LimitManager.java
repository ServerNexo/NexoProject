package me.nexo.protections.managers;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.protections.NexoProtections;

import java.util.UUID;

public class LimitManager {

    private final NexoProtections plugin;

    public LimitManager(NexoProtections plugin) {
        this.plugin = plugin;
    }

    /**
     * Calcula el límite máximo de bases que puede tener un jugador o su clan.
     */
    public int getMaxProtections(UUID playerId) {
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(playerId);
        if (user == null) return 2; // Límite base por si falla

        // 🌟 LÓGICA DE CLANES (Escala con el Monolito)
        if (user.hasClan()) {
            // Buscamos el clan en la caché de NexoClans extrayéndolo del Optional de forma segura
            NexoClan clan = NexoClans.getPlugin(NexoClans.class).getClanManager().getClanFromCache(user.getClanId()).orElse(null);

            if (clan != null) {
                // Nivel 1 = 2 Bases | Nivel 5 = 10 Bases (O el escalado que decidas)
                return 2 + (clan.getMonolithLevel() * 2);
            }
        }

        // 🌟 LÓGICA SOLITARIO (Límite estricto de 2 piedras)
        // TODO: Comprobar si el jugador tiene rango VIP para darle +1
        return 2;
    }

    public boolean canClaimMore(UUID playerId) {
        // Contamos cuántas piedras le pertenecen a este jugador iterando la caché RAM ultra rápida
        long currentBases = plugin.getClaimManager().getAllStones().values().stream()
                .filter(stone -> stone.getOwnerId().equals(playerId))
                .count();

        return currentBases < getMaxProtections(playerId);
    }
}