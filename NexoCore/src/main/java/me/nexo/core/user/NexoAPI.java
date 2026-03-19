package me.nexo.core.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NexoAPI {

    private static NexoAPI instance;
    private final UserManager userManager;

    public NexoAPI(UserManager userManager) {
        this.userManager = userManager;
        instance = this;
    }

    public static NexoAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("¡NexoAPI aún no ha sido inicializada en el Core!");
        }
        return instance;
    }

    // ==========================================
    // 🟢 LECTURA SEGURA (Read-Through)
    // ==========================================

    public NexoUser getUserLocal(UUID uuid) {
        return userManager.getUserOrNull(uuid);
    }

    public CompletableFuture<Optional<NexoUser>> getUserAsync(UUID uuid) {
        return CompletableFuture.completedFuture(userManager.getUser(uuid));
    }

    // ==========================================
    // 🟢 MODIFICACIÓN SEGURA (Addons)
    // ==========================================

    public CompletableFuture<Void> addCombateXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> user.addCombateXp(xp));
        });
    }

    public CompletableFuture<Void> addNexoXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> user.addNexoXp(xp));
        });
    }

    // 🌟 MÉTODOS PARA LOS MINIONS:
    public CompletableFuture<Void> addAgriculturaXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> user.addAgriculturaXp(xp));
        });
    }

    public CompletableFuture<Void> addMineriaXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> user.addMineriaXp(xp));
        });
    }
}