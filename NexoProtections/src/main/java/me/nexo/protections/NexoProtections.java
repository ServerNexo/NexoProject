package me.nexo.protections;

import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoProtections extends JavaPlugin {

    // 🌟 Variable estática para que NexoPvP y otros módulos puedan consultar las zonas seguras
    private static me.nexo.protections.managers.ClaimManager claimManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Iniciando NexoProtections (Motor Híbrido)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("NexoClans") == null) {
            getLogger().warning("⚠️ NexoClans no detectado. Las protecciones de clan estarán desactivadas.");
        }

        // 🌟 INICIALIZAMOS LOS MOTORES DE RAM Y LÍMITES
        claimManager = new me.nexo.protections.managers.ClaimManager();
        me.nexo.protections.managers.LimitManager limitManager = new me.nexo.protections.managers.LimitManager();

        // 🌟 CARGAR PIEDRAS DE SUPABASE A LA RAM AL ENCENDER
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        claimManager.loadAllStonesAsync(core);

        // 🌟 INICIAR EL CONSUMO DE ENERGÍA EN SEGUNDO PLANO
        me.nexo.protections.managers.UpkeepManager upkeepManager = new me.nexo.protections.managers.UpkeepManager(this, claimManager);

        // 🌟 REGISTRAMOS LOS LISTENERS
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionListener(claimManager, limitManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionMenuListener(claimManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.EnvironmentListener(claimManager), this);

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🛡️ NexoProtections apagado. La energía fue guardada asíncronamente por el Batch Update.");
    }

    // 🌟 Getter público para conectar con otros módulos
    public static me.nexo.protections.managers.ClaimManager getClaimManager() {
        return claimManager;
    }
}