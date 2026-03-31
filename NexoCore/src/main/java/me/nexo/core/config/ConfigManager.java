package me.nexo.core.config;

import me.nexo.core.NexoCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private final NexoCore plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    public ConfigManager(NexoCore plugin) {
        this.plugin = plugin;

        // ⚙️ Opcional pero recomendado: Cargar los archivos base al iniciar
        getConfig("config.yml");
        getConfig("messages.yml");
    }

    public FileConfiguration getConfig(String configName) {
        return configs.computeIfAbsent(configName, this::loadConfig);
    }

    private FileConfiguration loadConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);

        // 🛡️ REGLA OMEGA: ¡Proteger las credenciales de Supabase!
        if (configName.equalsIgnoreCase("config.yml")) {
            // Solo se crea si no existe. JAMÁS se sobreescribe.
            if (!configFile.exists()) {
                plugin.saveResource(configName, false);
            }
        }
        // 🌟 PROTOCOLO DE ACTUALIZACIÓN: messages.yml y otros archivos
        else {
            try {
                // Siempre extrae la versión más nueva del .jar (Color Lila Iluminado, nuevos textos, etc)
                plugin.saveResource(configName, true);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("No se pudo actualizar " + configName + " desde el jar.");
            }
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);

        // Si hacemos reload, también forzamos la actualización de los mensajes desde el .jar
        if (!configName.equalsIgnoreCase("config.yml")) {
            try {
                plugin.saveResource(configName, true);
            } catch (IllegalArgumentException ignored) {}
        }

        configs.put(configName, YamlConfiguration.loadConfiguration(configFile));
    }

    // 🛡️ MÉTODO ORIGINAL (Para cuando quieres leer un archivo específico)
    public String getMessage(String configName, String path) {
        return getConfig(configName).getString(path, "§cMensaje no encontrado: " + path);
    }

    // 🌟 NUEVO MÉTODO MÁGICO (Sobrecarga)
    // Cuando CommandNexo llama a getMessage("ruta"), entra aquí y busca por defecto en messages.yml
    public String getMessage(String path) {
        return getMessage("messages.yml", path);
    }
}