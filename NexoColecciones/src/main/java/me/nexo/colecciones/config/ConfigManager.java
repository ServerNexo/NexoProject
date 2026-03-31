package me.nexo.colecciones.config;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {

    private final NexoColecciones plugin;
    private FileConfiguration messagesConfig = null;
    private File messagesFile = null;

    public ConfigManager(NexoColecciones plugin) {
        this.plugin = plugin;
        saveDefaultMessages();
    }

    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        // 🌟 Recarga el archivo recién actualizado
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

        // 🌟 PROTOCOLO OMEGA ACTIVADO:
        // Al quitar el "if (!exists)" y poner "true", el servidor SIEMPRE
        // extraerá el messages.yml más nuevo de tu .jar y borrará el viejo.
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