package me.nexo.factories;

import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;
import me.nexo.factories.menu.FactoryMenu;
import me.nexo.factories.menu.LogicMenu; // 🌟 IMPORT AÑADIDO
import org.bukkit.plugin.java.JavaPlugin;

public class NexoFactories extends JavaPlugin {

    private FactoryManager factoryManager;
    private BlueprintManager blueprintManager;
    private LogicMenu logicMenu; // 🌟 NUEVA VARIABLE

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

        // 🌟 NUEVO: Inicializamos y registramos los Menús y el Detector de Clics
        FactoryMenu factoryMenu = new FactoryMenu(this);
        this.logicMenu = new LogicMenu(this); // 🌟 Inicializamos el menú de programación visual

        getServer().getPluginManager().registerEvents(factoryMenu, this);
        getServer().getPluginManager().registerEvents(this.logicMenu, this); // 🌟 Registramos el nuevo menú
        getServer().getPluginManager().registerEvents(new FactoryInteractListener(this, factoryMenu), this);

        // Registramos Comandos
        if (getCommand("factory") != null) {
            getCommand("factory").setExecutor(new ComandoFactory(this));
        }



        getLogger().info("✅ ¡NexoFactories cargado! Nexo-Grid en línea y produciendo.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏭 Apagando NexoFactories... Limpiando memoria.");
    }

    public FactoryManager getFactoryManager() { return factoryManager; }
    public BlueprintManager getBlueprintManager() { return blueprintManager; }
    public LogicMenu getLogicMenu() { return logicMenu; } // 🌟 NUEVO GETTER
}