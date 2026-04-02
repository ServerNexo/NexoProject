package me.nexo.mechanics;

import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.commands.ComandoSkillsTabCompleter;
import me.nexo.mechanics.config.ConfigManager; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMechanics extends JavaPlugin {

    // 🌟 VARIABLE AÑADIDA
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // 🌟 INICIALIZACIÓN OMEGA
        this.configManager = new ConfigManager(this);

        if (getCommand("skills") != null) {
            // Pasamos la instancia del plugin al comando
            getCommand("skills").setExecutor(new ComandoSkillTree(this));
            getCommand("skills").setTabCompleter(new ComandoSkillsTabCompleter());
        }

        getLogger().info("NexoMechanics ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexoMechanics ha sido deshabilitado.");
    }

    // 🌟 GETTER AÑADIDO
    public ConfigManager getConfigManager() {
        return configManager;
    }
}