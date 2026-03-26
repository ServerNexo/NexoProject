package me.nexo.protections;

import me.nexo.core.NexoCore;
import me.nexo.protections.commands.ComandoProteccion;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoProtections extends JavaPlugin {

    private static NexoProtections instance;
    private static me.nexo.protections.managers.ClaimManager claimManager;
    private me.nexo.protections.managers.LimitManager limitManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("🛡️ Iniciando NexoProtections (Motor Híbrido)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializar Motores
        claimManager = new me.nexo.protections.managers.ClaimManager();
        limitManager = new me.nexo.protections.managers.LimitManager(this);

        // Cargar Base de Datos
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        claimManager.loadAllStonesAsync(core);

        // Iniciar Consumo
        new me.nexo.protections.managers.UpkeepManager(this, claimManager);

        // Registrar Listeners
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionListener(claimManager, limitManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionMenuListener(claimManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.EnvironmentListener(claimManager), this);

        // Registrar Comando Único
        if (getCommand("nexo") != null) {
            getCommand("nexo").setExecutor(new ComandoProteccion(this));
        }

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🛡️ NexoProtections apagado. La energía fue guardada asíncronamente.");
    }

    // 🌟 COMANDO RELOAD GLOBAL
    public void reloadSystem() {
        getLogger().info("🔄 Recargando NexoProtections...");
        reloadConfig();
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        // Limpiamos la RAM y volvemos a descargar todo de Supabase
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync(core);
    }

    public static NexoProtections getInstance() { return instance; }
    public static me.nexo.protections.managers.ClaimManager getClaimManager() { return claimManager; }
}