package me.nexo.protections;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI;
import me.nexo.protections.commands.ComandoProteccion;
import me.nexo.protections.commands.ComandoProteccionTabCompleter;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoProtections extends JavaPlugin {

    private static NexoProtections instance;
    private static ClaimManager claimManager;
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

        claimManager = new ClaimManager();
        limitManager = new me.nexo.protections.managers.LimitManager(this);

        NexoAPI.getServices().register(ClaimManager.class, claimManager);

        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        claimManager.loadAllStonesAsync(core);

        new me.nexo.protections.managers.UpkeepManager(this, claimManager);

        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionListener(claimManager, limitManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.ProtectionMenuListener(claimManager), this);
        getServer().getPluginManager().registerEvents(new me.nexo.protections.listeners.EnvironmentListener(claimManager), this);

        if (getCommand("nexo") != null) {
            getCommand("nexo").setExecutor(new ComandoProteccion(this));
            getCommand("nexo").setTabCompleter(new ComandoProteccionTabCompleter());
        }

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        NexoAPI.getServices().unregister(ClaimManager.class);
        getLogger().info("🛡️ NexoProtections apagado. La energía fue guardada asíncronamente.");
    }

    public void reloadSystem() {
        getLogger().info("🔄 Recargando NexoProtections...");
        reloadConfig();
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync(core);
    }

    public static NexoProtections getInstance() { return instance; }
    public static ClaimManager getClaimManager() { return claimManager; }
}