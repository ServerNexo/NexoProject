package me.nexo.colecciones;

import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoSlayer; // 🌟 NUEVO IMPORT PARA EL COMANDO SLAYER
import me.nexo.colecciones.menu.MenuListener;
// 🌟 IMPORTS PARA SLAYERS
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoColecciones extends JavaPlugin {

    private ColeccionesConfig coleccionesConfig;
    private CollectionManager collectionManager; // El Cerebro de la progresión
    private SlayerManager slayerManager;        // 🌟 El Gremio de Cazadores (Slayers)
    private FlushTask flushTask;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 NexoColecciones activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // ==========================================
        // 1. INICIALIZAR CONFIGURACIÓN Y MANAGERS
        // ==========================================
        this.coleccionesConfig = new ColeccionesConfig(this);

        // Inicializamos los sistemas de datos
        this.collectionManager = new CollectionManager(this);
        this.slayerManager = new SlayerManager(this); // 🌟 Inicializamos Slayers

        // ==========================================
        // 2. REGISTRAR EVENTOS Y COMANDOS
        // ==========================================
        // Registro de Colecciones
        getServer().getPluginManager().registerEvents(new ColeccionesListener(this), this);

        // 🌟 Registro de Slayers
        getServer().getPluginManager().registerEvents(new SlayerListener(this), this);

        // Protección de menús
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // Registro del comando central /colecciones
        if (getCommand("colecciones") != null) {
            getCommand("colecciones").setExecutor(new ComandoColecciones(this));
        } else {
            getLogger().warning("⚠️ No se pudo registrar /colecciones. Verifica tu plugin.yml.");
        }

        // 🌟 NUEVO: Registro del comando independiente /slayer
        if (getCommand("slayer") != null) {
            getCommand("slayer").setExecutor(new ComandoSlayer(this));
        } else {
            getLogger().warning("⚠️ No se pudo registrar /slayer. Verifica tu plugin.yml.");
        }

        // ==========================================
        // 3. CONECTAR A LA BASE DE DATOS Y AUTO-GUARDADO
        // ==========================================
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        if (core.getDatabaseManager() != null && core.getDatabaseManager().getDataSource() != null) {
            this.flushTask = new FlushTask(core.getDatabaseManager().getDataSource());
            // Guardar en la base de datos cada 10 minutos
            this.flushTask.runTaskTimerAsynchronously(this, 12000L, 12000L);
        } else {
            getLogger().severe("¡No se pudo conectar a la base de datos de NexoCore!");
        }

        // ==========================================
        // 4. INTEGRACIÓN CON PLACEHOLDERAPI
        // ==========================================
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new me.nexo.colecciones.api.ColeccionesExpansion(this).register();
            getLogger().info("🌟 ¡PlaceholderAPI detectado y variables registradas!");
        } else {
            getLogger().warning("⚠️ PlaceholderAPI no encontrado. Las variables no funcionarán.");
        }

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        // Guardado de emergencia al apagar el servidor
        if (flushTask != null) {
            flushTask.run();
            getLogger().info("💾 Progreso de Colecciones y Slayers guardado correctamente.");
        }
        getLogger().info("📚 NexoColecciones apagado.");
    }

    public ColeccionesConfig getColeccionesConfig() {
        return coleccionesConfig;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    // 🌟 Nuevo Getter para acceder al sistema de Slayers
    public SlayerManager getSlayerManager() {
        return slayerManager;
    }
}