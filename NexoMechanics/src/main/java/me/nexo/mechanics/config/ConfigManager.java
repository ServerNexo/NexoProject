package me.nexo.mechanics.config;

import me.nexo.mechanics.NexoMechanics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {

    private final NexoMechanics plugin;
    private FileConfiguration messagesConfig = null;
    private File messagesFile = null;

    public ConfigManager(NexoMechanics plugin) {
        this.plugin = plugin;
        saveDefaultMessages();
    }

    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getMessages() {
        if (messagesConfig == null) {
            reloadMessages();
        }
        return messagesConfig;
    }

    public void saveDefaultMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        // 🌟 PROTOCOLO OMEGA ACTIVADO
        try {
            plugin.saveResource("messages.yml", true);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("No se pudo actualizar messages.yml desde el jar.");
        }
    }

    public String getMessage(String path) {
        return getMessages().getString(path, "&cMessage not found: " + path);
    }
}