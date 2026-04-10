package me.nexo.minions;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.di.MinionsModule;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMinions extends JavaPlugin {

    private Injector injector;
    private MinionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🤖 Iniciando NexoMinions (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 Inicializar Inyección
        this.injector = Guice.createInjector(new MinionsModule(this));

        // Forzamos la carga de configuraciones
        injector.getInstance(TiersConfig.class);
        injector.getInstance(UpgradesConfig.class);

        // 🚀 Arrancar Orquestador
        this.bootstrap = injector.getInstance(MinionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoMinions cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS
    // ==========================================
    public MinionManager getMinionManager() {
        return injector.getInstance(MinionManager.class);
    }

    public TiersConfig getTiersConfig() {
        return injector.getInstance(TiersConfig.class);
    }

    public UpgradesConfig getUpgradesConfig() {
        return injector.getInstance(UpgradesConfig.class);
    }

    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }
}