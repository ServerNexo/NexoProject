package me.nexo.core.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class UserManager {

    // 🟢 CLEAN CODE: Caché ultra rápida que expira sola en 2 horas si el jugador no hace nada
    // Esto previene los temidos Memory Leaks si un evento falla al desconectarse el jugador.
    private final Cache<UUID, NexoUser> usersCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    public UserManager() {
        // Constructor vacío por ahora
    }

    /**
     * Guarda a un jugador en la caché local (RAM).
     */
    public void addUserToCache(NexoUser user) {
        if (user != null) {
            usersCache.put(user.getUuid(), user);
        }
    }

    /**
     * Remueve a un jugador de la caché local (RAM).
     */
    public void removeUserFromCache(UUID uuid) {
        usersCache.invalidate(uuid);
    }

    /**
     * Obtiene a un jugador de la caché.
     * Devuelve Optional para obligar al programador a revisar si el jugador realmente existe.
     */
    public Optional<NexoUser> getUser(UUID uuid) {
        return Optional.ofNullable(usersCache.getIfPresent(uuid));
    }

    /**
     * Obtiene a un jugador directamente (puede ser nulo).
     */
    public NexoUser getUserOrNull(UUID uuid) {
        return usersCache.getIfPresent(uuid);
    }
}