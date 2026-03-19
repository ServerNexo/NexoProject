package me.nexo.minions.data;

import me.nexo.minions.NexoMinions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TiersConfig {
    private final NexoMinions plugin;
    private FileConfiguration config;

    public TiersConfig(NexoMinions plugin) {
        this.plugin = plugin;
        cargarConfig();
    }

    private void cargarConfig() {
        File configFile = new File(plugin.getDataFolder(), "tiers.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("tiers.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Busca cuánto cuesta subir al siguiente nivel
    public ConfigurationSection getCostoEvolucion(MinionType tipo, int siguienteNivel) {
        return config.getConfigurationSection(tipo.name() + "." + siguienteNivel);
    }
}