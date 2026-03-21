package me.nexo.clans.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClanManager {

    private final NexoClans plugin;
    private final NexoCore core;

    private final Cache<UUID, NexoClan> clanCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public ClanManager(NexoClans plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        crearTablaClanes(); // 🌟 CREAMOS LA TABLA AL INICIAR
    }

    // ==========================================
    // 🗄️ BASE DE DATOS: CREACIÓN DE TABLA
    // ==========================================
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
                        public_home TEXT DEFAULT NULL
                    );
                    """;
            try (Connection conn = core.getDatabaseManager().getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (Exception e) {
                plugin.getLogger().severe("Error creando tabla nexo_clans: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🛠️ LÓGICA DE CREACIÓN DE CLAN
    // ==========================================
    public void crearClanAsync(Player player, NexoUser user, String tag, String nombre) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1. Verificar si el Tag o Nombre ya existen
            String checkSQL = "SELECT id FROM nexo_clans WHERE tag = ? OR name = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement psCheck = conn.prepareStatement(checkSQL)) {

                psCheck.setString(1, tag);
                psCheck.setString(2, nombre);
                ResultSet rs = psCheck.executeQuery();

                if (rs.next()) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§c¡Ese Tag o Nombre ya está en uso por otro clan!"));
                    return;
                }

                // 2. Crear el Clan en la base de datos
                UUID nuevoClanId = UUID.randomUUID();
                String insertClan = "INSERT INTO nexo_clans (id, name, tag) VALUES (CAST(? AS UUID), ?, ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertClan)) {
                    psInsert.setString(1, nuevoClanId.toString());
                    psInsert.setString(2, nombre);
                    psInsert.setString(3, tag);
                    psInsert.executeUpdate();
                }

                // 3. Actualizar al jugador en la BD (Hacerlo Lider)
                String updateUser = "UPDATE jugadores SET clan_id = CAST(? AS UUID), clan_role = 'LIDER' WHERE uuid = ?";
                try (PreparedStatement psUser = conn.prepareStatement(updateUser)) {
                    psUser.setString(1, nuevoClanId.toString());
                    psUser.setString(2, player.getUniqueId().toString());
                    psUser.executeUpdate();
                }

                // 4. Volver al Hilo Principal y actualizar la Memoria RAM
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Creamos el objeto en RAM
                    NexoClan nuevoClan = new NexoClan(nuevoClanId, nombre, tag, 1, 0L, BigDecimal.ZERO, null);
                    clanCache.put(nuevoClanId, nuevoClan);

                    // Actualizamos al usuario en RAM
                    user.setClanId(nuevoClanId);
                    user.setClanRole("LIDER");

                    player.sendMessage("§8=========================");
                    player.sendMessage("§a¡Has fundado el clan §e" + nombre + " §8[§7" + tag + "§8]§a!");
                    player.sendMessage("§7Usa §e/clan §7para ver tu nuevo gremio.");
                    player.sendMessage("§8=========================");
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cError crítico al crear el clan. Contacta a un administrador."));
                plugin.getLogger().severe("Error al crear clan: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🧠 MÉTODOS DE CACHÉ EXISTENTES
    // ==========================================
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
                            rs.getBigDecimal("bank_balance"), rs.getString("public_home")
                    );
                    clanCache.put(clanId, loadedClan);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(loadedClan));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando clan: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public Optional<NexoClan> getClanFromCache(UUID clanId) {
        return Optional.ofNullable(clanCache.getIfPresent(clanId));
    }
}