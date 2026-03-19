package me.nexo.colecciones;

import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoColecciones extends JavaPlugin {

    private ColeccionesConfig coleccionesConfig;
    private FlushTask flushTask;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 NexoColecciones activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // ==========================================
        // 1. INICIALIZAR CONFIGURACIÓN Y ARCHIVOS
        // ==========================================
        this.coleccionesConfig = new ColeccionesConfig(this);

        // ==========================================
        // 2. REGISTRAR EVENTOS
        // ==========================================
        getServer().getPluginManager().registerEvents(new ColeccionesListener(this, this.coleccionesConfig), this);

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
}