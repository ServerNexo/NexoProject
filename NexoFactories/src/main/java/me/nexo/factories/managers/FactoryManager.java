package me.nexo.factories.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 🏭 NexoFactories - Manager Central de Máquinas (Arquitectura Enterprise)
 */
@Singleton
public class FactoryManager {

    private final NexoFactories plugin;
    private final Cache<UUID, ActiveFactory> factoryCache;
    private final ScriptEvaluator logicEngine;

    private static final double ENERGY_COST_PER_CYCLE = 15.0;
    private static final long CYCLE_DURATION_MS = 60_000L;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public FactoryManager(NexoFactories plugin) {
        this.plugin = plugin;
        this.logicEngine = new ScriptEvaluator();
        this.factoryCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public CompletableFuture<Void> loadFactoriesAsync() {
        return CompletableFuture.runAsync(() -> {
            String sql = "SELECT * FROM nexo_factories";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String[] locParts = rs.getString("core_location").split(",");
                    World world = Bukkit.getWorld(locParts[0]);
                    if (world == null) continue;
                    Location coreLocation = new Location(world, Double.parseDouble(locParts[1]), Double.parseDouble(locParts[2]), Double.parseDouble(locParts[3]));

                    ActiveFactory factory = new ActiveFactory(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("stone_id")),
                            UUID.fromString(rs.getString("owner_id")),
                            rs.getString("factory_type"),
                            rs.getInt("level"),
                            rs.getString("current_status"),
                            rs.getInt("stored_output"),
                            coreLocation,
                            rs.getString("catalyst_item"),
                            rs.getString("json_logic"),
                            rs.getLong("last_evaluation")
                    );
                    factoryCache.put(factory.getId(), factory);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading factories from database", e);
            }
        });
    }

    public void tickFactories() {
        long now = System.currentTimeMillis();
        factoryCache.asMap().values().forEach(factory -> CompletableFuture.runAsync(() -> {
            long diff = now - factory.getLastEvaluationTime();
            if (diff < CYCLE_DURATION_MS) return;

            long cycles = diff / CYCLE_DURATION_MS;
            NexoAPI.getServices().get(ClaimManager.class).ifPresent(claimManager -> {
                ProtectionStone stone = claimManager.getStoneById(factory.getStoneId());

                if (stone == null) {
                    factory.setCurrentStatus("NO_STONE");
                    return;
                }

                if (!logicEngine.shouldRun(factory, stone, factory.getJsonLogic())) {
                    factory.setCurrentStatus("SCRIPT_PAUSED");
                    return;
                }

                double requiredEnergy = ENERGY_COST_PER_CYCLE * cycles;
                long actualCycles = (stone.getCurrentEnergy() < requiredEnergy) ? (long) (stone.getCurrentEnergy() / ENERGY_COST_PER_CYCLE) : cycles;

                if (actualCycles > 0) {
                    stone.drainEnergy(ENERGY_COST_PER_CYCLE * actualCycles);
                    double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());
                    if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                        multiplier += 0.5;
                    }
                    int finalOutput = (int) Math.round((factory.getLevel() * 2) * multiplier * actualCycles);
                    factory.addOutput(finalOutput);
                }

                factory.setCurrentStatus(actualCycles == cycles ? "ACTIVE" : "NO_ENERGY");
                factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
                saveFactoryStatusAsync(factory);
            });
        }));
    }

    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(ownerId);
            if (user != null) {
                int level = 1;
                if (factoryType.contains("MINA") || factoryType.contains("FORJA"))
                    level = user.getSkillLevel(Skills.MINING);
                else if (factoryType.contains("ASERRADERO")) level = user.getSkillLevel(Skills.FORAGING);
                else if (factoryType.contains("GRANJA")) level = user.getSkillLevel(Skills.FARMING);
                return 1.0 + (level * 0.02);
            }
        } catch (NoClassDefFoundError | IllegalStateException ignored) {
        }
        return 1.0;
    }

    public CompletableFuture<Void> createFactoryAsync(ActiveFactory factory) {
        factory.setLastEvaluationTime(System.currentTimeMillis());
        factoryCache.put(factory.getId(), factory);
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location, last_evaluation, catalyst_item, json_logic) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, factory.getId().toString());
                ps.setString(2, factory.getStoneId().toString());
                ps.setString(3, factory.getOwnerId().toString());
                ps.setString(4, factory.getFactoryType());
                ps.setInt(5, factory.getLevel());
                ps.setString(6, factory.getCurrentStatus());
                ps.setInt(7, factory.getStoredOutput());
                String locStr = factory.getCoreLocation().getWorld().getName() + "," + factory.getCoreLocation().getBlockX() + "," + factory.getCoreLocation().getBlockY() + "," + factory.getCoreLocation().getBlockZ();
                ps.setString(8, locStr);
                ps.setLong(9, factory.getLastEvaluationTime());
                ps.setString(10, factory.getCatalystItem());
                ps.setString(11, factory.getJsonLogic());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error creating factory in database", e);
            }
        });
    }

    public void saveFactoryStatusAsync(ActiveFactory factory) {
        CompletableFuture.runAsync(() -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving factory status to database", e);
            }
        });
    }

    public void saveAllFactoriesSync() {
        String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
        try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ActiveFactory factory : factoryCache.asMap().values()) {
                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error on synchronous save of factories", e);
        }
    }

    public ActiveFactory getFactoryAt(Location loc) {
        for (ActiveFactory factory : factoryCache.asMap().values()) {
            if (factory.getCoreLocation() != null && factory.getCoreLocation().equals(loc)) {
                return factory;
            }
        }
        return null;
    }
}