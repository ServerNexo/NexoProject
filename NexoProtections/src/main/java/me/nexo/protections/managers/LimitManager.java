package me.nexo.protections.managers;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class LimitManager {

    private final NexoCore core;

    public LimitManager() {
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    // 🌟 Retorna un Future para no congelar el servidor mientras lee la base de datos
    public CompletableFuture<Boolean> canPlaceNewStone(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
            if (user == null) return false;

            try (Connection conn = core.getDatabaseManager().getConnection()) {
                if (user.hasClan()) {
                    // LÍMITE DE CLAN: (Nivel / 5) + 1
                    int clanLevel = 1;
                    String sqlClan = "SELECT monolith_level FROM nexo_clans WHERE id = CAST(? AS UUID)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlClan)) {
                        ps.setString(1, user.getClanId().toString());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) clanLevel = rs.getInt("monolith_level");
                    }

                    int maxStones = (clanLevel / 5) + 1;

                    String sqlCount = "SELECT COUNT(*) FROM nexo_protections WHERE clan_id = CAST(? AS UUID)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                        ps.setString(1, user.getClanId().toString());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getInt(1) < maxStones;
                    }
                } else {
                    // LÍMITE SOLITARIO: Máximo 2
                    String sqlCount = "SELECT COUNT(*) FROM nexo_protections WHERE owner_id = CAST(? AS UUID) AND clan_id IS NULL";
                    try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                        ps.setString(1, player.getUniqueId().toString());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getInt(1) < 2;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public int getProtectionRadius(Player player) {
        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
        if (user != null && user.hasClan()) {
            // Piedra de Clan: Por ahora dejaremos un Radio de 25 bloques (51x51)
            return 25;
        }
        // Piedra Solitaria: Radio de 7 bloques (15x15)
        return 7;
    }
}