package me.nexo.clans.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClanManager {

    private final NexoClans plugin;
    private final NexoCore core;

    private final Cache<UUID, NexoClan> clanCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private final Cache<UUID, UUID> invitaciones = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    public ClanManager(NexoClans plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        crearTablaClanes();
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("clans_messages.yml", path);
    }

    private void crearTablaClanes() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                    CREATE TABLE IF NOT EXISTS nexo_clans (
                        id UUID PRIMARY KEY,
                        name VARCHAR(32) UNIQUE NOT NULL,
                        tag VARCHAR(5) UNIQUE NOT NULL,
                        monolith_level INT DEFAULT 1,
                        monolith_exp BIGINT DEFAULT 0,
                        bank_balance DECIMAL(15,2) DEFAULT 0.00,
                        public_home TEXT DEFAULT NULL,
                        friendly_fire BOOLEAN DEFAULT FALSE
                    );
                    """;
            try (Connection conn = core.getDatabaseManager().getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                try {
                    stmt.execute("ALTER TABLE nexo_clans ADD COLUMN friendly_fire BOOLEAN DEFAULT FALSE;");
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error creando tabla nexo_clans: " + e.getMessage());
            }
        });
    }

    public void setClanHomeAsync(NexoClan clan, Player player, Location loc) {
        String locStr = loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_clans SET public_home = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, locStr);
                ps.setString(2, clan.getId().toString());
                ps.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    clan.setPublicHome(locStr);
                    CrossplayUtils.sendMessage(player, getMessage("comandos.clan.exito.home-establecido"));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error guardando Base: " + e.getMessage());
            }
        });
    }

    public void toggleFriendlyFireAsync(NexoClan clan, Player player, boolean newValue) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_clans SET friendly_fire = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, newValue);
                ps.setString(2, clan.getId().toString());
                ps.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    clan.setFriendlyFire(newValue);
                    String colorStatus = newValue ? "&#8b0000<bold>RIESGO ACTIVO</bold>" : "&#00f5ff<bold>APAGADO</bold>";
                    CrossplayUtils.sendMessage(player, getMessage("comandos.clan.feedback.ff-toggle").replace("%status%", colorStatus));
                });
            } catch (Exception e) {
            }
        });
    }

    public void getMiembrosAsync(UUID clanId, java.util.function.Consumer<List<ClanMember>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ClanMember> miembros = new ArrayList<>();
            String sql = "SELECT uuid, name, clan_role FROM jugadores WHERE clan_id = CAST(? AS UUID) ORDER BY clan_role DESC";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, clanId.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    miembros.add(new ClanMember(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getString("clan_role")
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando miembros: " + e.getMessage());
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(miembros));
        });
    }

    public void crearClanAsync(Player player, NexoUser user, String tag, String nombre) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String checkSQL = "SELECT id FROM nexo_clans WHERE tag = ? OR name = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement psCheck = conn.prepareStatement(checkSQL)) {
                psCheck.setString(1, tag);
                psCheck.setString(2, nombre);
                if (psCheck.executeQuery().next()) {
                    Bukkit.getScheduler().runTask(plugin, () -> CrossplayUtils.sendMessage(player, getMessage("comandos.clan.errores.nombre-tomado")));
                    return;
                }
                UUID nuevoClanId = UUID.randomUUID();
                String insertClan = "INSERT INTO nexo_clans (id, name, tag) VALUES (CAST(? AS UUID), ?, ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertClan)) {
                    psInsert.setString(1, nuevoClanId.toString());
                    psInsert.setString(2, nombre);
                    psInsert.setString(3, tag);
                    psInsert.executeUpdate();
                }
                String updateUser = "UPDATE jugadores SET clan_id = CAST(? AS UUID), clan_role = 'LIDER' WHERE uuid = ?";
                try (PreparedStatement psUser = conn.prepareStatement(updateUser)) {
                    psUser.setString(1, nuevoClanId.toString());
                    psUser.setString(2, player.getUniqueId().toString());
                    psUser.executeUpdate();
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    NexoClan nuevoClan = new NexoClan(nuevoClanId, nombre, tag, 1, 0L, BigDecimal.ZERO, null, false);
                    clanCache.put(nuevoClanId, nuevoClan);
                    user.setClanId(nuevoClanId);
                    user.setClanRole("LIDER");
                    CrossplayUtils.sendMessage(player, getMessage("comandos.clan.exito.clan-creado").replace("%name%", nombre).replace("%tag%", tag));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error creando clan: " + e.getMessage());
            }
        });
    }

    public void loadClanAsync(UUID clanId, java.util.function.Consumer<NexoClan> callback) {
        NexoClan cached = clanCache.getIfPresent(clanId);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT * FROM nexo_clans WHERE id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, clanId.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    NexoClan loadedClan = new NexoClan(
                            clanId, rs.getString("name"), rs.getString("tag"),
                            rs.getInt("monolith_level"), rs.getLong("monolith_exp"),
                            rs.getBigDecimal("bank_balance"), rs.getString("public_home"),
                            rs.getBoolean("friendly_fire")
                    );
                    clanCache.put(clanId, loadedClan);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(loadedClan));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void invitarJugador(Player lider, Player invitado, NexoClan clan) {
        invitaciones.put(invitado.getUniqueId(), clan.getId());
        CrossplayUtils.sendMessage(invitado, getMessage("comandos.clan.feedback.invitacion-recibida").replace("%clan%", clan.getName()));
        CrossplayUtils.sendMessage(lider, getMessage("comandos.clan.exito.invitacion-enviada").replace("%player%", invitado.getName()));
    }

    public UUID getInvitacionPendiente(Player player) {
        return invitaciones.getIfPresent(player.getUniqueId());
    }

    public void unirseClanAsync(Player player, NexoUser user, NexoClan clan) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String updateSQL = "UPDATE jugadores SET clan_id = CAST(? AS UUID), clan_role = 'MIEMBRO' WHERE uuid = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, clan.getId().toString());
                ps.setString(2, player.getUniqueId().toString());
                ps.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    user.setClanId(clan.getId());
                    user.setClanRole("MIEMBRO");
                    invitaciones.invalidate(player.getUniqueId());
                    CrossplayUtils.sendMessage(player, getMessage("comandos.clan.exito.unido").replace("%clan%", clan.getName()));
                });
            } catch (Exception e) {
            }
        });
    }

    public void abandonarClanAsync(Player player, NexoUser user) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String updateSQL = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE uuid = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    user.setClanId(null);
                    user.setClanRole("NONE");
                    CrossplayUtils.sendMessage(player, getMessage("comandos.clan.exito.abandonado"));
                });
            } catch (Exception e) {
            }
        });
    }

    public void expulsarJugadorAsync(Player ejector, Player target, NexoUser targetUser) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String updateSQL = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE uuid = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, target.getUniqueId().toString());
                ps.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    targetUser.setClanId(null);
                    targetUser.setClanRole("NONE");
                    CrossplayUtils.sendMessage(target, getMessage("comandos.clan.feedback.expulsado-feedback").replace("%ejector%", ejector.getName()));
                    CrossplayUtils.sendMessage(ejector, getMessage("comandos.clan.exito.expulsado").replace("%target%", target.getName()));
                });
            } catch (Exception e) {
            }
        });
    }

    public void disolverClanAsync(Player lider, NexoUser liderUser, UUID clanId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = core.getDatabaseManager().getConnection()) {
                String updateUsers = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE clan_id = CAST(? AS UUID)";
                try (PreparedStatement ps = conn.prepareStatement(updateUsers)) {
                    ps.setString(1, clanId.toString());
                    ps.executeUpdate();
                }
                String deleteClan = "DELETE FROM nexo_clans WHERE id = CAST(? AS UUID)";
                try (PreparedStatement ps = conn.prepareStatement(deleteClan)) {
                    ps.setString(1, clanId.toString());
                    ps.executeUpdate();
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    clanCache.invalidate(clanId);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        NexoUser u = core.getUserManager().getUserOrNull(p.getUniqueId());
                        if (u != null && u.hasClan() && u.getClanId().equals(clanId)) {
                            u.setClanId(null);
                            u.setClanRole("NONE");
                            CrossplayUtils.sendMessage(p, getMessage("comandos.clan.feedback.disuelto"));
                        }
                    }
                });
            } catch (Exception e) {
            }
        });
    }

    public Optional<NexoClan> getClanFromCache(UUID clanId) {
        return Optional.ofNullable(clanCache.getIfPresent(clanId));
    }

    public void saveBankAsync(NexoClan clan) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, clan.getBankBalance());
                ps.setString(2, clan.getId().toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Error guardando el banco del clan: " + e.getMessage());
            }
        });
    }

    public void saveAllClansSync() {
        String sql = "UPDATE nexo_clans SET monolith_exp = ?, monolith_level = ?, bank_balance = ?, public_home = ?, friendly_fire = ? WHERE id = CAST(? AS UUID)";
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (NexoClan clan : clanCache.asMap().values()) {
                ps.setLong(1, clan.getMonolithExp());
                ps.setInt(2, clan.getMonolithLevel());
                ps.setBigDecimal(3, clan.getBankBalance());
                ps.setString(4, clan.getPublicHome());
                ps.setBoolean(5, clan.isFriendlyFire());
                ps.setString(6, clan.getId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            plugin.getLogger().severe("Error en el guardado síncrono de clanes: " + e.getMessage());
        }
    }
}