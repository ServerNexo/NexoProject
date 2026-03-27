package me.nexo.colecciones.colecciones;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionProfile {
    private final UUID playerUUID;

    // Memoria de cantidad recolectada (ID de Colección -> Cantidad)
    private final ConcurrentHashMap<String, Integer> progress;

    // 🌟 NUEVO: Memoria de Tiers reclamados manualmente (ID de Colección -> Set de Niveles cobrados)
    private final ConcurrentHashMap<String, Set<Integer>> claimedTiers;

    private boolean needsFlush = false;

    public CollectionProfile(UUID playerUUID, Map<String, Integer> loadedProgress, Map<String, Set<Integer>> loadedClaimedTiers) {
        this.playerUUID = playerUUID;
        this.progress = loadedProgress != null ? new ConcurrentHashMap<>(loadedProgress) : new ConcurrentHashMap<>();
        this.claimedTiers = loadedClaimedTiers != null ? new ConcurrentHashMap<>(loadedClaimedTiers) : new ConcurrentHashMap<>();
    }

    // ==========================================================
    // 🧮 GESTIÓN DE PROGRESO BASE
    // ==========================================================

    // Método para leer el progreso actual en la RAM
    public int getProgress(String id) {
        return this.progress.getOrDefault(id, 0);
    }

    // Método para añadir progreso silenciosamente (SIN RECLAMO AUTOMÁTICO)
    public void addProgress(String id, int amount) {
        int oldAmount = progress.getOrDefault(id, 0);
        progress.put(id, oldAmount + amount);
        this.needsFlush = true; // Avisa al FlushTask que debe guardar esto
    }

    // Fija un progreso exacto (útil para comandos de Admin)
    public void setProgress(String id, int amount) {
        progress.put(id, amount);
        this.needsFlush = true;
    }

    // ==========================================================
    // 🎁 GESTIÓN DE TIERS (RECLAMO MANUAL)
    // ==========================================================

    // 🌟 NUEVO: Verifica si un jugador ya cobró un Nivel en específico
    public boolean hasClaimedTier(String collectionId, int tierLevel) {
        Set<Integer> claimed = claimedTiers.get(collectionId);
        return claimed != null && claimed.contains(tierLevel);
    }

    // 🌟 NUEVO: Marca un Nivel como cobrado permanentemente
    public void markTierAsClaimed(String collectionId, int tierLevel) {
        // Si no existe un Set para esta colección, lo crea, y luego añade el Nivel
        claimedTiers.computeIfAbsent(collectionId, k -> new HashSet<>()).add(tierLevel);
        this.needsFlush = true;
    }

    // ==========================================================
    // ⚙️ GETTERS DE ARQUITECTURA (Para Base de Datos)
    // ==========================================================

    public boolean isNeedsFlush() { return needsFlush; }
    public void setNeedsFlush(boolean needsFlush) { this.needsFlush = needsFlush; }

    public ConcurrentHashMap<String, Integer> getProgressMap() { return progress; }
    public ConcurrentHashMap<String, Set<Integer>> getClaimedTiersMap() { return claimedTiers; }
    public UUID getPlayerUUID() { return playerUUID; }
}