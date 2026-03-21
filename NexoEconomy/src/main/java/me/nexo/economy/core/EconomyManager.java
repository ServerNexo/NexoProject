package me.nexo.economy.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.core.NexoCore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class EconomyManager {

    private final JavaPlugin plugin;
    private final NexoCore core;

    // ⚡ Caché Ultrarrápido: Las cuentas expiran a los 30 min de inactividad
    private final Cache<String, NexoAccount> accountCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        crearTablaEconomia();
    }

    private void crearTablaEconomia() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                    CREATE TABLE IF NOT EXISTS nexo_economy (
                        id UUID PRIMARY KEY,
                        account_type VARCHAR(20) NOT NULL,
                        coins DECIMAL(20,2) DEFAULT 0.00,
                        gems DECIMAL(20,2) DEFAULT 0.00,
                        mana DECIMAL(20,2) DEFAULT 0.00
                    );
                    """;
            try (Connection conn = core.getDatabaseManager().getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (Exception e) {
                plugin.getLogger().severe("Error creando tabla de economía: " + e.getMessage());
            }
        });
    }

    /**
     * Identificador único para el Caché (Ej: "PLAYER:uuid" o "CLAN:uuid")
     */
    private String getCacheKey(UUID id, NexoAccount.AccountType type) {
        return type.name() + ":" + id.toString();
    }

    /**
     * Carga una cuenta desde la DB (o la crea si no existe)
     */
    public CompletableFuture<NexoAccount> getAccountAsync(UUID ownerId, NexoAccount.AccountType type) {
        String cacheKey = getCacheKey(ownerId, type);
        NexoAccount cached = accountCache.getIfPresent(cacheKey);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = core.getDatabaseManager().getConnection()) {
                // Intentamos buscarla
                String selectSQL = "SELECT * FROM nexo_economy WHERE id = CAST(? AS UUID)";
                try (PreparedStatement ps = conn.prepareStatement(selectSQL)) {
                    ps.setString(1, ownerId.toString());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        NexoAccount acc = new NexoAccount(
                                ownerId, type,
                                rs.getBigDecimal("coins"),
                                rs.getBigDecimal("gems"),
                                rs.getBigDecimal("mana")
                        );
                        accountCache.put(cacheKey, acc);
                        return acc;
                    }
                }

                // Si no existe, la creamos (Balance Inicial: 0)
                String insertSQL = "INSERT INTO nexo_economy (id, account_type) VALUES (CAST(? AS UUID), ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                    ps.setString(1, ownerId.toString());
                    ps.setString(2, type.name());
                    ps.executeUpdate();

                    NexoAccount newAcc = new NexoAccount(ownerId, type, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    accountCache.put(cacheKey, newAcc);
                    return newAcc;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando cuenta: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * 🛡️ TRANSACCIÓN ATÓMICA: Actualiza la DB y el Caché de forma segura
     */
    public CompletableFuture<Boolean> updateBalanceAsync(UUID ownerId, NexoAccount.AccountType type, NexoAccount.Currency currency, BigDecimal amount, boolean isDeposit) {
        return getAccountAsync(ownerId, type).thenApplyAsync(account -> {
            if (account == null) return false;

            if (!isDeposit && !account.hasEnough(currency, amount)) {
                return false; // No tiene fondos suficientes
            }

            // Nombre exacto de la columna en la BD
            String column = currency.name().toLowerCase();
            String operator = isDeposit ? "+" : "-";

            String updateSQL = "UPDATE nexo_economy SET " + column + " = " + column + " " + operator + " ? WHERE id = CAST(? AS UUID)";

            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {

                ps.setBigDecimal(1, amount);
                ps.setString(2, ownerId.toString());
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    // Solo si la BD se actualizó correctamente, actualizamos la RAM (Evita duplicaciones por lag)
                    if (isDeposit) account.addBalance(currency, amount);
                    else account.removeBalance(currency, amount);
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Fallo de Transacción Atómica: " + e.getMessage());
            }
            return false;
        });
    }

    public Optional<NexoAccount> getCachedAccount(UUID ownerId, NexoAccount.AccountType type) {
        return Optional.ofNullable(accountCache.getIfPresent(getCacheKey(ownerId, type)));
    }
}