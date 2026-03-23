package me.nexo.factories;

import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;
import me.nexo.factories.menu.FactoryMenu;
import me.nexo.factories.tasks.ProductionCycleTask;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoFactories extends JavaPlugin {

    private FactoryManager factoryManager;
    private BlueprintManager blueprintManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏭 Iniciando NexoFactories (Motor Industrial Zero-Lag)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null ||
                getServer().getPluginManager().getPlugin("NexoProtections") == null) {
            getLogger().severe("❌ Error: Faltan NexoCore o NexoProtections.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializamos Cerebros
        this.factoryManager = new FactoryManager(this);
        this.blueprintManager = new BlueprintManager(this);

        // Registramos Listeners base
        getServer().getPluginManager().registerEvents(blueprintManager, this);

        // 🌟 NUEVO: Inicializamos y registramos el Menú y el Detector de Clics
        FactoryMenu factoryMenu = new FactoryMenu(this);
        getServer().getPluginManager().registerEvents(factoryMenu, this);
        getServer().getPluginManager().registerEvents(new FactoryInteractListener(this, factoryMenu), this);

        // Registramos Comandos
        if (getCommand("factory") != null) {
            getCommand("factory").setExecutor(new ComandoFactory(this));
        }

        // 🌟 ENCENDEMOS EL MOTOR DE PRODUCCIÓN (El Nexo-Grid)
        // Se ejecutará cada 1200 ticks (60 segundos)
        new ProductionCycleTask(this).runTaskTimer(this, 1200L, 1200L);

        getLogger().info("✅ ¡NexoFactories cargado! Nexo-Grid en línea y produciendo.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏭 Apagando NexoFactories... Limpiando memoria.");
    }

    public FactoryManager getFactoryManager() { return factoryManager; }
    public BlueprintManager getBlueprintManager() { return blueprintManager; }
}