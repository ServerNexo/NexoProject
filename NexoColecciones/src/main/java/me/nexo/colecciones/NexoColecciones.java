package me.nexo.colecciones;

import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
// 🌟 NUEVOS IMPORTS PARA EL MENÚ Y EL COMANDO
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.menu.MenuListener;
import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoColecciones extends JavaPlugin {

    private ColeccionesConfig coleccionesConfig;
    private CollectionManager collectionManager; // 🌟 El Cerebro de la progresión
    private FlushTask flushTask;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 NexoColecciones activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // ==========================================
        // 1. INICIALIZAR CONFIGURACIÓN Y EL MANAGER
        // ==========================================
        this.coleccionesConfig = new ColeccionesConfig(this);

        // Inicializamos el sistema que maneja los 15 niveles
        this.collectionManager = new CollectionManager(this);

        // ==========================================
        // 2. REGISTRAR EVENTOS Y COMANDOS
        // ==========================================
        // El Listener de colecciones ahora vigila el Anti-Exploit
        getServer().getPluginManager().registerEvents(new ColeccionesListener(this), this);

        // 🌟 NUEVO: Registramos la protección del menú para que no roben ítems
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // 🌟 NUEVO: Registramos el comando /colecciones
        if (getCommand("colecciones") != null) {
            getCommand("colecciones").setExecutor(new ComandoColecciones(this));
        } else {
            getLogger().warning("⚠️ No se pudo registrar /colecciones. Verifica tu plugin.yml.");
        }

        // ==========================================
        // 3. CONECTAR A LA BASE DE DATOS Y AUTO-GUARDADO
        // ==========================================
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        if (core.getDatabaseManager() != null && core.getDatabaseManager().getDataSource() != null) {
            this.flushTask = new FlushTask(core.getDatabaseManager().getDataSource());
            // Guardar en Supabase cada 10 minutos (12000 ticks)
            this.flushTask.runTaskTimerAsynchronously(this, 12000L, 12000L);
        } else {
            getLogger().severe("¡No se pudo conectar a la base de datos de NexoCore!");
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

    // 🌟 Método para que otros archivos (como FlushTask y ColeccionesListener) puedan usar el cerebro
    public CollectionManager getCollectionManager() {
        return collectionManager;
    }
}