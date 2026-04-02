package me.nexo.war;

import me.nexo.war.commands.ComandoWar;
import me.nexo.war.commands.ComandoWarTabCompleter;
import me.nexo.war.config.ConfigManager; // 🌟 IMPORTACIÓN AÑADIDA
import me.nexo.war.listeners.WarCrossplayListener;
import me.nexo.war.listeners.WarListener;
import me.nexo.war.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoWar extends JavaPlugin {

    // 🌟 DECLARACIÓN DEL CONFIG MANAGER
    private ConfigManager configManager;
    private WarManager warManager;

    @Override
    public void onEnable() {
        // 🌟 INICIALIZAR EL CONFIG MANAGER PRIMERO
        this.configManager = new ConfigManager(this);

        this.warManager = new WarManager(this);

        getServer().getPluginManager().registerEvents(new WarListener(this), this);
        getServer().getPluginManager().registerEvents(new WarCrossplayListener(), this);

        if (getCommand("war") != null) {
            getCommand("war").setExecutor(new ComandoWar(this));
            getCommand("war").setTabCompleter(new ComandoWarTabCompleter());
        }

        getLogger().info("NexoWar ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexoWar ha sido deshabilitado.");
    }

    public WarManager getWarManager() {
        return warManager;
    }

    // 🌟 GETTER AÑADIDO PARA LEER LOS MENSAJES
    public ConfigManager getConfigManager() {
        return configManager;
    }
}