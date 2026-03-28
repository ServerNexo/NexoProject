package me.nexo.factories.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.NexoCore;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FactoryManager {

    private final NexoFactories plugin;
    private final Cache<UUID, ActiveFactory> factoryCache;

    // ⏳ Parámetros de la Evaluación Pasiva (Nexo Architect V3.0)
    private final ScriptEvaluator logicEngine;
    private static final double ENERGY_COST_PER_CYCLE = 15.0;
    private static final long CYCLE_DURATION_MS = 60_000L; // 1 ciclo = 60 segundos (1 minuto)

    public FactoryManager(NexoFactories plugin) {
        this.plugin = plugin;
        this.logicEngine = new ScriptEvaluator();
        this.factoryCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000) // Soporta hasta 10,000 máquinas cargadas en RAM
                .build();
    }

    // ==========================================
    // ⏳ MOTOR DE PRODUCCIÓN PASIVA (Timestamp Diff)
    // ==========================================
    public void evaluateOfflineProduction(ActiveFactory factory) {
        long now = System.currentTimeMillis();
        long diff = now - factory.getLastEvaluationTime();
        long cycles = diff / CYCLE_DURATION_MS;

        // Si no ha pasado ni 1 minuto, no hacemos nada (Evita micro-cálculos innecesarios)
        if (cycles <= 0) return;

        // Consultamos el núcleo de protección donde está conectada
        ProtectionStone stone = NexoProtections.getPlugin(NexoProtections.class).getClaimManager().getStoneById(factory.getStoneId());
        if (stone == null) {
            factory.setCurrentStatus("NO_STONE");
            return;
        }

        // 1. EL MOTOR LÓGICO: ¿La máquina debería estar encendida según su programación?
        if (!logicEngine.shouldRun(factory, stone, factory.getJsonLogic())) {
            factory.setCurrentStatus("SCRIPT_PAUSED");
            return;
        }

        // 2. CÁLCULO DE ENERGÍA Y CICLOS REALES
        double requiredEnergy = ENERGY_COST_PER_CYCLE * cycles;
        long actualCycles = cycles;

        // Si la piedra no tiene suficiente energía para todos los ciclos que estuvo desconectado
        if (stone.getCurrentEnergy() < requiredEnergy) {
            actualCycles = (long) (stone.getCurrentEnergy() / ENERGY_COST_PER_CYCLE); // Hace los ciclos que pueda
            factory.setCurrentStatus("NO_ENERGY");
        } else {
            factory.setCurrentStatus("ACTIVE");
        }

        // 3. GENERACIÓN MASIVA (Zero-Tick Loop)
        if (actualCycles > 0) {
            stone.drainEnergy(ENERGY_COST_PER_CYCLE * actualCycles);

            int baseProduction = factory.getLevel() * 2;
            double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());

            // ⚡ COMPONENTE DE HARDWARE: Catalizador Overclock, +50% de producción
            if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                multiplier += 0.5;
            }

            int finalOutput = (int) Math.round(baseProduction * multiplier * actualCycles);
            factory.addOutput(finalOutput);

            // Actualizamos el reloj, dejando el "residuo" de tiempo intacto para el próximo ciclo
            factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));

            // Guardamos el resultado en la base de datos de fondo
            saveFactoryStatusAsync(factory);
        }
    }

    // 🎓 INTEGRACIÓN RPG: El nivel de AuraSkills mejora las fábricas de los jugadores
    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(ownerId);
            if (user != null) {
                int level = 1;
                if (factoryType.contains("MINA") || factoryType.contains("FORJA")) level = user.getSkillLevel(Skills.MINING);
                else if (factoryType.contains("ASERRADERO")) level = user.getSkillLevel(Skills.FORAGING);
                else if (factoryType.contains("GRANJA")) level = user.getSkillLevel(Skills.FARMING);

                return 1.0 + (level * 0.02); // +2% de producción por cada nivel
            }
        } catch (NoClassDefFoundError | IllegalStateException ignored) {
            // Falla silenciosa si AuraSkills no está cargado
        }
        return 1.0;
    }

    // ==========================================
    // 💾 GESTIÓN DE BASE DE DATOS Y CACHÉ
    // ==========================================
    public void createFactoryAsync(ActiveFactory factory) {
        factory.setLastEvaluationTime(System.currentTimeMillis()); // 🌟 Inicia el reloj
        factoryCache.put(factory.getId(), factory);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location, last_evaluation) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getId().toString());
                ps.setString(2, factory.getStoneId().toString());
                ps.setString(3, factory.getOwnerId().toString());
                ps.setString(4, factory.getFactoryType());
                ps.setInt(5, factory.getLevel());
                ps.setString(6, factory.getCurrentStatus());
                ps.setInt(7, factory.getStoredOutput());

                String locStr = factory.getCoreLocation().getWorld().getName() + "," +
                        factory.getCoreLocation().getBlockX() + "," +
                        factory.getCoreLocation().getBlockY() + "," +
                        factory.getCoreLocation().getBlockZ();
                ps.setString(8, locStr);

                ps.setLong(9, factory.getLastEvaluationTime()); // 🌟 Guardar Timestamp

                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public void saveFactoryStatusAsync(ActiveFactory factory) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime()); // 🌟 Mantenemos el tiempo
                ps.setString(4, factory.getId().toString());

                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public ActiveFactory getFactoryFromCache(UUID id) {
        ActiveFactory factory = factoryCache.getIfPresent(id);
        if (factory != null) evaluateOfflineProduction(factory); // 🌟 Evalúa automáticamente al consultar
        return factory;
    }

    public ActiveFactory getFactoryAt(Location loc) {
        for (ActiveFactory factory : factoryCache.asMap().values()) {
            if (factory.getCoreLocation() != null &&
                    factory.getCoreLocation().getWorld().equals(loc.getWorld()) &&
                    factory.getCoreLocation().getBlockX() == loc.getBlockX() &&
                    factory.getCoreLocation().getBlockY() == loc.getBlockY() &&
                    factory.getCoreLocation().getBlockZ() == loc.getBlockZ()) {

                evaluateOfflineProduction(factory); // 🌟 Evalúa el paso del tiempo justo antes de devolverla
                return factory;
            }
        }
        return null;
    }

    public Cache<UUID, ActiveFactory> getCache() {
        return factoryCache;
    }
}