package me.nexo.factories.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.core.NexoCore;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FactoryManager {

    private final NexoFactories plugin;

    // 🚀 CACHÉ ULTRA RÁPIDA: Guarda las máquinas en RAM.
    // Si no se usan/procesan en 30 minutos, se borran de la RAM para liberar memoria.
    private final Cache<UUID, ActiveFactory> factoryCache;

    public FactoryManager(NexoFactories plugin) {
        this.plugin = plugin;
        this.factoryCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000) // Soporta hasta 10,000 máquinas cargadas a la vez
                .build();
    }

    // 💾 GUARDAR NUEVA MÁQUINA EN LA BASE DE DATOS
    public void createFactoryAsync(ActiveFactory factory) {
        // La guardamos en RAM instantáneamente
        factoryCache.put(factory.getId(), factory);

        // Y le decimos a un hilo secundario que la guarde en Supabase
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 🌟 ACTUALIZADO: Ahora guarda core_location
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getId().toString());
                ps.setString(2, factory.getStoneId().toString());
                ps.setString(3, factory.getOwnerId().toString());
                ps.setString(4, factory.getFactoryType());
                ps.setInt(5, factory.getLevel());
                ps.setString(6, factory.getCurrentStatus());
                ps.setInt(7, factory.getStoredOutput());

                // Formateamos la ubicación a Texto (Mundo,X,Y,Z)
                String locStr = factory.getCoreLocation().getWorld().getName() + "," +
                        factory.getCoreLocation().getBlockX() + "," +
                        factory.getCoreLocation().getBlockY() + "," +
                        factory.getCoreLocation().getBlockZ();
                ps.setString(8, locStr);

                ps.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 🔄 ACTUALIZAR ESTADO (Guardar el Output generado sin dar lag)
    public void saveFactoryStatusAsync(ActiveFactory factory) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_tick = CURRENT_TIMESTAMP WHERE id = CAST(? AS UUID)";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setString(3, factory.getId().toString());
                ps.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ActiveFactory getFactoryFromCache(UUID id) {
        return factoryCache.getIfPresent(id);
    }

    public Cache<UUID, ActiveFactory> getCache() {
        return factoryCache;
    }

    // 🌟 NUEVO: Busca una máquina cargada en memoria según la ubicación del bloque central
    public ActiveFactory getFactoryAt(Location loc) {
        for (ActiveFactory factory : factoryCache.asMap().values()) {
            if (factory.getCoreLocation() != null &&
                    factory.getCoreLocation().getWorld().equals(loc.getWorld()) &&
                    factory.getCoreLocation().getBlockX() == loc.getBlockX() &&
                    factory.getCoreLocation().getBlockY() == loc.getBlockY() &&
                    factory.getCoreLocation().getBlockZ() == loc.getBlockZ()) {
                return factory;
            }
        }
        return null;
    }
}