package me.nexo.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DatabaseManager {

    private final NexoCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(NexoCore plugin) {
        this.plugin = plugin;
    }

    // 🛡️ MÉTODO BLINDADO CONTRA CRASHEOS DE CONEXIÓN
    public void conectar() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(plugin.getConfig().getString("database.url"));
            config.setUsername(plugin.getConfig().getString("database.username"));
            config.setPassword(plugin.getConfig().getString("database.password"));

            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000); // Máximo 10 segundos esperando para no congelar el servidor

            dataSource = new HikariDataSource(config);
            crearTabla(); // Aquí llamamos a la creación de tablas

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida correctamente!");

        } catch (Exception e) {
            plugin.getLogger().severe("============================================");
            plugin.getLogger().severe("❌ ERROR DE BASE DE DATOS SUPABASE ❌");
            plugin.getLogger().severe("No se pudo conectar a la base de datos: " + e.getMessage());
            plugin.getLogger().severe("Revisa que tu contraseña sea correcta o que Supabase no esté pausado.");
            plugin.getLogger().severe("El plugin encenderá, pero los datos no se guardarán en la nube.");
            plugin.getLogger().severe("============================================");
        }
    }

    public void desconectar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("El DataSource no está inicializado.");
        return dataSource.getConnection();
    }

    // Método agregado para que NexoCollections pueda usar HikariCP
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    // ==========================================
    // 🗄️ CREACIÓN DE MÚLTIPLES TABLAS (Optimizadas Java 21)
    // ==========================================
    private void crearTabla() {
        if (dataSource == null) return; // Si falló la conexión, no intentamos crear tablas

        // 🟢 Clean Code: Text Blocks para evitar el "+" infinito en SQL + CLANES + BENDICIONES
        String sqlJugadores = """
                CREATE TABLE IF NOT EXISTS jugadores (
                    uuid VARCHAR(36) PRIMARY KEY, nombre VARCHAR(16) NOT NULL,
                    nexo_nivel INT DEFAULT 1, nexo_xp INT DEFAULT 0,
                    combate_nivel INT DEFAULT 1, combate_xp INT DEFAULT 0,
                    mineria_nivel INT DEFAULT 1, mineria_xp INT DEFAULT 0,
                    agricultura_nivel INT DEFAULT 1, agricultura_xp INT DEFAULT 0,
                    clan_id UUID DEFAULT NULL, clan_role VARCHAR(15) DEFAULT 'NONE',
                    blessings TEXT DEFAULT ''
                );""";

        String sqlMochilas = """
                CREATE TABLE IF NOT EXISTS mochilas (
                    uuid VARCHAR(36), mochila_id INT, contenido TEXT,
                    PRIMARY KEY (uuid, mochila_id)
                );""";

        String sqlGuardarropa = """
                CREATE TABLE IF NOT EXISTS guardarropa (
                    uuid VARCHAR(36), preset_id INT, contenido TEXT,
                    PRIMARY KEY (uuid, preset_id)
                );""";

        String sqlStorage = """
                CREATE TABLE IF NOT EXISTS nexo_storage (
                    uuid VARCHAR(36), tipo VARCHAR(32), contenido TEXT,
                    PRIMARY KEY (uuid, tipo)
                );""";

        String sqlColecciones = """
                CREATE TABLE IF NOT EXISTS nexo_collections (
                    uuid VARCHAR(36) PRIMARY KEY,
                    collections_data JSONB NOT NULL DEFAULT '{}'::jsonb
                );""";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {

                // Ejecutamos las creaciones al arrancar el servidor
                stmt.execute(sqlJugadores);

                // 🌟 MÓDULO 5: Inyección Silenciosa de la columna Blessings por si la tabla ya existía previamente
                try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS blessings TEXT DEFAULT '';"); } catch (Exception ignored) {}

                stmt.execute(sqlMochilas);
                stmt.execute(sqlGuardarropa);
                stmt.execute(sqlStorage);
                stmt.execute(sqlColecciones);
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 👤 GESTIÓN DE JUGADOR (Nueva Arquitectura NexoUser)
    // ==========================================
    public void cargarJugador(Player player) {
        if (dataSource == null) return;

        String selectSQL = "SELECT * FROM jugadores WHERE uuid = ?";
        String insertSQL = "INSERT INTO jugadores (uuid, nombre) VALUES (?, ?)";
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement psSelect = conn.prepareStatement(selectSQL)) {
                    psSelect.setString(1, uuid.toString());
                    ResultSet rs = psSelect.executeQuery();

                    if (rs.next()) {
                        // 🟢 LECTURA DE CLAN: Evitamos NullPointerExceptions
                        String clanIdStr = rs.getString("clan_id");
                        UUID clanId = (clanIdStr != null && !clanIdStr.isEmpty()) ? UUID.fromString(clanIdStr) : null;
                        String clanRole = rs.getString("clan_role");

                        // 🌟 LECTURA DE BENDICIONES
                        String blessingsRaw = rs.getString("blessings");

                        // 🟢 ARQUITECTURA: Creamos el objeto NexoUser con los datos de la DB
                        NexoUser user = new NexoUser(
                                uuid, name,
                                rs.getInt("nexo_nivel"), rs.getInt("nexo_xp"),
                                rs.getInt("combate_nivel"), rs.getInt("combate_xp"),
                                rs.getInt("mineria_nivel"), rs.getInt("mineria_xp"),
                                rs.getInt("agricultura_nivel"), rs.getInt("agricultura_xp"),
                                clanId, clanRole
                        );

                        // Inyectar bendiciones al caché
                        if (blessingsRaw != null && !blessingsRaw.isEmpty()) {
                            Set<String> blessSet = new HashSet<>(Arrays.asList(blessingsRaw.split(",")));
                            user.setBlessings(blessSet);
                        }

                        // 🔴 VOLVEMOS AL MAIN THREAD para meterlo en la caché de forma segura
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getUserManager().addUserToCache(user);
                        });

                    } else {
                        // Jugador Nuevo
                        try (PreparedStatement psInsert = conn.prepareStatement(insertSQL)) {
                            psInsert.setString(1, uuid.toString());
                            psInsert.setString(2, name);
                            psInsert.executeUpdate();

                            // 🔴 VOLVEMOS AL MAIN THREAD
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                // Se crea por defecto sin clan (null, "NONE")
                                NexoUser newUser = new NexoUser(
                                        uuid, name, 1, 0, 1, 0, 1, 0, 1, 0, null, "NONE"
                                );
                                plugin.getUserManager().addUserToCache(newUser);
                                plugin.getLogger().info("¡Nuevo jugador RPG registrado: " + name);
                            });
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al cargar jugador " + name + ": " + e.getMessage());
            }
        });
    }

    public void guardarJugador(Player player) {
        if (dataSource == null) return;
        UUID uuid = player.getUniqueId();

        // 🟢 ARQUITECTURA: Pedimos el usuario a la caché
        NexoUser user = plugin.getUserManager().getUserOrNull(uuid);
        if (user == null) return; // Si no está en caché, no hay nada que guardar

        // 🟢 Actualizado para guardar Clan ID, Rol y Bendiciones
        String updateSQL = """
                UPDATE jugadores SET nexo_nivel = ?, nexo_xp = ?, nombre = ?, 
                combate_nivel = ?, combate_xp = ?, mineria_nivel = ?, mineria_xp = ?, 
                agricultura_nivel = ?, agricultura_xp = ?, clan_id = CAST(? AS UUID), clan_role = ?,
                blessings = ?
                WHERE uuid = ?
                """;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {

                ps.setInt(1, user.getNexoNivel()); ps.setInt(2, user.getNexoXp()); ps.setString(3, user.getNombre());
                ps.setInt(4, user.getCombateNivel()); ps.setInt(5, user.getCombateXp()); ps.setInt(6, user.getMineriaNivel());
                ps.setInt(7, user.getMineriaXp()); ps.setInt(8, user.getAgriculturaNivel()); ps.setInt(9, user.getAgriculturaXp());

                // Si no tiene clan, insertamos un NULL
                if (user.getClanId() != null) {
                    ps.setString(10, user.getClanId().toString());
                } else {
                    ps.setNull(10, Types.VARCHAR);
                }

                ps.setString(11, user.getClanRole());
                ps.setString(12, String.join(",", user.getActiveBlessings())); // 🌟 Serializar Bendiciones
                ps.setString(13, uuid.toString());

                ps.executeUpdate();

                // 🔴 ANTI DATA LOSS: Solo borramos de la RAM si se guardó con éxito
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getUserManager().removeUserFromCache(uuid);
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("Fallo grave al guardar a " + user.getNombre() + ". Datos preservados en memoria: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🔴 NUEVO MÉTODO CRÍTICO PARA EL APAGADO (onDisable)
    // ==========================================
    public void guardarJugadorSync(Player player) {
        if (dataSource == null) return;
        UUID uuid = player.getUniqueId();

        NexoUser user = plugin.getUserManager().getUserOrNull(uuid);
        if (user == null) return;

        // 🟢 Actualizado para guardar Clan ID, Rol y Bendiciones síncronamente
        String updateSQL = """
                UPDATE jugadores SET nexo_nivel = ?, nexo_xp = ?, nombre = ?, 
                combate_nivel = ?, combate_xp = ?, mineria_nivel = ?, mineria_xp = ?, 
                agricultura_nivel = ?, agricultura_xp = ?, clan_id = CAST(? AS UUID), clan_role = ?,
                blessings = ?
                WHERE uuid = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSQL)) {

            ps.setInt(1, user.getNexoNivel()); ps.setInt(2, user.getNexoXp()); ps.setString(3, user.getNombre());
            ps.setInt(4, user.getCombateNivel()); ps.setInt(5, user.getCombateXp()); ps.setInt(6, user.getMineriaNivel());
            ps.setInt(7, user.getMineriaXp()); ps.setInt(8, user.getAgriculturaNivel()); ps.setInt(9, user.getAgriculturaXp());

            if (user.getClanId() != null) {
                ps.setString(10, user.getClanId().toString());
            } else {
                ps.setNull(10, Types.VARCHAR);
            }

            ps.setString(11, user.getClanRole());
            ps.setString(12, String.join(",", user.getActiveBlessings())); // 🌟 Serializar Bendiciones
            ps.setString(13, uuid.toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en guardado síncrono de " + player.getName() + ": " + e.getMessage());
        }
    }

    // ==========================================
    // ⚡ TAREA ZERO-MAIN-THREAD: GUARDADO RÁPIDO DE BENDICIONES
    // ==========================================
    public void saveUserBlessings(NexoUser user) {
        if (dataSource == null || user == null) return;
        String updateSQL = "UPDATE jugadores SET blessings = ? WHERE uuid = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, String.join(",", user.getActiveBlessings()));
                ps.setString(2, user.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error actualizando bendición de " + user.getNombre() + ": " + e.getMessage());
            }
        });
    }
}