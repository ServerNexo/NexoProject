package me.nexo.protections.core;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionStone {
    private final UUID stoneId;
    private final UUID ownerId;
    private final UUID clanId; // Nullable (Si es null, es una piedra solitaria)
    private final ClaimBox box;

    // Sistema de Mantenimiento (Upkeep)
    private double currentEnergy;
    private double maxEnergy;

    private final Set<UUID> trustedFriends = new HashSet<>();
    private final Map<String, Boolean> environmentFlags = new ConcurrentHashMap<>();

    public ProtectionStone(UUID stoneId, UUID ownerId, UUID clanId, ClaimBox box) {
        this.stoneId = stoneId;
        this.ownerId = ownerId;
        this.clanId = clanId;
        this.box = box;

        this.currentEnergy = 100.0;
        this.maxEnergy = 1000.0;

        // Flags por defecto de una zona segura
        this.environmentFlags.put("pvp", false);
        this.environmentFlags.put("mob-spawning", false);
        this.environmentFlags.put("tnt-damage", false);
        this.environmentFlags.put("fire-spread", false);
    }

    // ==========================================
    // 🛡️ MATRIZ DE PERMISOS HÍBRIDA (RBAC)
    // ==========================================
    public boolean hasPermission(UUID playerId, ClaimAction action) {
        // 1. El dueño absoluto (el que la colocó) siempre tiene permiso total
        if (playerId.equals(ownerId)) return true;

        // 2. Si la piedra NO tiene energía, las defensas caen y se vuelve "ruinas" vulnerables
        if (currentEnergy <= 0) return true;

        // 3. Lógica Solitaria (Sin Clan)
        if (clanId == null) {
            // Si es su amigo, le dejamos hacer todo. Si no, bloqueado.
            return trustedFriends.contains(playerId);
        }
        // 4. Lógica de Clan
        else {
            me.nexo.core.NexoCore core = me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class);
            if (core != null) {
                me.nexo.core.user.NexoUser user = core.getUserManager().getUserOrNull(playerId);

                // Verificamos si el usuario está en la memoria, si tiene clan y si es el clan de esta piedra
                if (user != null && user.hasClan() && clanId.equals(user.getClanId())) {
                    String role = user.getClanRole().toUpperCase();

                    // Líderes y Oficiales tienen acceso total para construir y destruir
                    if (role.equals("LIDER") || role.equals("OFICIAL")) return true;

                    // Miembros normales solo pueden interactuar y abrir cofres/puertas
                    if (role.equals("MIEMBRO") && (action == ClaimAction.INTERACT || action == ClaimAction.OPEN_CONTAINER)) return true;
                }
            }
            return false;
        }
    }

    // ==========================================
    // 🔋 GETTERS Y SETTERS
    // ==========================================
    public ClaimBox getBox() { return box; }
    public UUID getStoneId() { return stoneId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getClanId() { return clanId; }

    public double getCurrentEnergy() { return currentEnergy; }
    public double getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(double maxEnergy) { this.maxEnergy = maxEnergy; }

    public void addEnergy(double amount) { this.currentEnergy = Math.min(maxEnergy, this.currentEnergy + amount); }
    public void drainEnergy(double amount) { this.currentEnergy = Math.max(0, this.currentEnergy - amount); }

    public boolean getFlag(String flagName) { return environmentFlags.getOrDefault(flagName, true); }
    public void setFlag(String flagName, boolean value) { environmentFlags.put(flagName, value); }
    public Map<String, Boolean> getFlags() { return environmentFlags; }

    public Set<UUID> getTrustedFriends() { return trustedFriends; }
    public void addFriend(UUID friendId) { trustedFriends.add(friendId); }
    public void removeFriend(UUID friendId) { trustedFriends.remove(friendId); }
}