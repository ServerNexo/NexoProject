package me.nexo.core.config;

import me.nexo.core.NexoCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private final NexoCore plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    public ConfigManager(NexoCore plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration getConfig(String configName) {
        return configs.computeIfAbsent(configName, this::loadConfig);
    }

    private FileConfiguration loadConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);
        if (!configFile.exists()) {
            plugin.saveResource(configName, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);
        configs.put(configName, YamlConfiguration.loadConfiguration(configFile));
    }

    public String getMessage(String configName, String path) {
        return getConfig(configName).getString(path, "&cMessage not found: " + path);
    }
}