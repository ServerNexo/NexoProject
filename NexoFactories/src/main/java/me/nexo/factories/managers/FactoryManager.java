package me.nexo.factories.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.database.DatabaseManager;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.logic.ScriptEvaluator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 🏭 NexoFactories - Manager Central de Máquinas (Arquitectura Enterprise)
 * Rendimiento: Hilo Virtual Único (Tick Engine), Spatial Grid O(1) y SQL Batching.
 */
@Singleton
public class FactoryManager {

    private final NexoFactories plugin;
    private final DatabaseManager databaseManager;
    private final ScriptEvaluator logicEngine;

    // Caché principal (Memoria Volátil Rápida)
    private final Cache<UUID, ActiveFactory> factoryCache;

    // 🌟 OPTIMIZACIÓN O(1): Mapa espacial para búsquedas instantáneas de coordenadas
    private final Map<String, ActiveFactory> locationMap = new ConcurrentHashMap<>();

    private static final double ENERGY_COST_PER_CYCLE = 15.0;
    private static final long CYCLE_DURATION_MS = 60_000L; // 1 Minuto por ciclo

    // 💉 PILAR 3: Inyección de Dependencias Directa (Cero acoplamiento a NexoCore)
    @Inject
    public FactoryManager(NexoFactories plugin, DatabaseManager databaseManager, ScriptEvaluator logicEngine) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.logicEngine = logicEngine;

        this.factoryCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .removalListener((key, value, cause) -> {
                    if (value instanceof ActiveFactory factory) {
                        locationMap.remove(serializeLocation(factory.getCoreLocation()));
                    }
                })
                .build();
    }

    // ==========================================
    // 🗄️ CARGA Y GUARDADO ASÍNCRONO
    // ==========================================
    public CompletableFuture<Void> loadFactoriesAsync() {
        return CompletableFuture.runAsync(() -> {
            String sql = "SELECT * FROM nexo_factories";
            try (Connection conn = databaseManager.getConnection();
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
                    locationMap.put(serializeLocation(coreLocation), factory);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error cargando las fábricas desde la DB", e);
            }
        });
    }

    // ==========================================
    // ⚙️ MOTOR INDUSTRIAL (Tick Engine)
    // ==========================================
    public void tickFactories() {
        // 🌟 MAGIA ENTERPRISE: Un solo Hilo Virtual evalúa todas las fábricas en serie.
        // Las matemáticas en memoria RAM toman microsegundos. Evita la "Explosión de Hilos" del ForkJoinPool.
        Thread.startVirtualThread(() -> {
            long now = System.currentTimeMillis();

            // 🌟 FIX DESACOPLAMIENTO: Extraemos el ClaimManager por reflexión para evitar el error de Maven
            Object claimManagerObj = null;
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                    claimManagerObj = me.nexo.core.user.NexoAPI.getServices()
                            .get(Class.forName("me.nexo.protections.managers.ClaimManager"))
                            .orElse(null);
                }
            } catch (ClassNotFoundException ignored) {}

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                long diff = now - factory.getLastEvaluationTime();
                if (diff < CYCLE_DURATION_MS) continue; // Aún no le toca ciclo

                long cycles = diff / CYCLE_DURATION_MS;

                // Si no tenemos el ClaimManager, asumimos que no hay escudo de energía (Fábrica Gratis)
                if (claimManagerObj == null) {
                    procesarProduccion(factory, cycles, now, diff, 1000000.0); // Energía infinita simulada
                    continue;
                }

                // Reflexión para obtener la piedra y la energía (Desacoplado)
                try {
                    Object stone = claimManagerObj.getClass().getMethod("getStoneById", UUID.class).invoke(claimManagerObj, factory.getStoneId());

                    if (stone == null) {
                        factory.setCurrentStatus("NO_STONE");
                        continue;
                    }

                    double currentEnergy = (double) stone.getClass().getMethod("getCurrentEnergy").invoke(stone);

                    // 🧠 Evaluador Lógico Cacheado O(1)
                    if (!logicEngine.shouldRun(factory, null, factory.getJsonLogic())) {
                        factory.setCurrentStatus("SCRIPT_PAUSED");
                        continue;
                    }

                    procesarProduccion(factory, cycles, now, diff, currentEnergy);

                } catch (Exception e) {
                    factory.setCurrentStatus("ERROR");
                }
            }
        });
    }

    private void procesarProduccion(ActiveFactory factory, long cycles, long now, long diff, double availableEnergy) {
        double requiredEnergy = ENERGY_COST_PER_CYCLE * cycles;
        long actualCycles = (availableEnergy < requiredEnergy) ? (long) (availableEnergy / ENERGY_COST_PER_CYCLE) : cycles;

        if (actualCycles > 0) {
            // Nota: Aquí deberías drenar la energía de la piedra si NexoProtections está presente,
            // pero para no acoplar, es mejor que las fábricas solo consuman y que el UpkeepManager de Protections las cobre luego.

            double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());
            if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                multiplier += 0.5;
            }
            int finalOutput = (int) Math.round((factory.getLevel() * 2) * multiplier * actualCycles);
            factory.addOutput(finalOutput);
        }

        factory.setCurrentStatus(actualCycles == cycles ? "ACTIVE" : "NO_ENERGY");
        factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
        saveFactoryStatusAsync(factory); // Auto-guardado de la máquina procesada
    }

    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        if (!Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) return 1.0;

        try {
            SkillsUser user = AuraSkillsApi.get().getUser(ownerId);
            if (user != null) {
                int level = 1;
                // Ajustamos a strings seguros para evitar nulls
                if (factoryType.contains("MINA") || factoryType.contains("FORJA"))
                    level = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.MINING);
                else if (factoryType.contains("ASERRADERO"))
                    level = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FORAGING);
                else if (factoryType.contains("GRANJA"))
                    level = user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FARMING);

                return 1.0 + (level * 0.02); // 2% de bonus por nivel de habilidad
            }
        } catch (NoClassDefFoundError | IllegalStateException ignored) {}

        return 1.0;
    }

    // ==========================================
    // 🗄️ SQL Y GUARDADOS EN BATCH
    // ==========================================
    public CompletableFuture<Void> createFactoryAsync(ActiveFactory factory) {
        factory.setLastEvaluationTime(System.currentTimeMillis());
        factoryCache.put(factory.getId(), factory);
        locationMap.put(serializeLocation(factory.getCoreLocation()), factory); // 🌟 Actualizamos caché O(1)

        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location, last_evaluation, catalyst_item, json_logic) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getId().toString());
                ps.setString(2, factory.getStoneId().toString());
                ps.setString(3, factory.getOwnerId().toString());
                ps.setString(4, factory.getFactoryType());
                ps.setInt(5, factory.getLevel());
                ps.setString(6, factory.getCurrentStatus());
                ps.setInt(7, factory.getStoredOutput());
                ps.setString(8, serializeLocation(factory.getCoreLocation()));
                ps.setLong(9, factory.getLastEvaluationTime());
                ps.setString(10, factory.getCatalystItem());
                ps.setString(11, factory.getJsonLogic());

                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error creando fábrica en la DB", e);
            }
        });
    }

    public void saveFactoryStatusAsync(ActiveFactory factory) {
        Thread.startVirtualThread(() -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }

    public void saveAllFactoriesSync() {
        String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 🌟 OPTIMIZACIÓN: Batch Mode para no saturar el I/O del disco duro

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            plugin.getLogger().info("💾 [AUTO-SAVE] Progreso industrial de todas las fábricas guardado en lote.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error en guardado síncrono", e);
        }
    }

    // ==========================================
    // 📍 BÚSQUEDAS ESPACIALES O(1)
    // ==========================================
    public ActiveFactory getFactoryAt(Location loc) {
        // 🌟 Búsqueda instantánea O(1) usando el mapa espacial en lugar de un bucle For O(N)
        return locationMap.get(serializeLocation(loc));
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}