package me.nexo.colecciones.colecciones;

// 🟢 ARQUITECTURA: Importamos el plugin principal de tu nuevo Addon
import me.nexo.colecciones.NexoColecciones;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ColeccionesConfig {

    // 🟢 ARQUITECTURA: Cambiamos Main por NexoColecciones
    private final NexoColecciones plugin;
    private FileConfiguration config;
    private File configFile;

    // 🟢 ARQUITECTURA: Pedimos NexoColecciones en el constructor
    public ColeccionesConfig(NexoColecciones plugin) {
        this.plugin = plugin;
        crearConfig();
    }

    public void crearConfig() {
        configFile = new File(plugin.getDataFolder(), "colecciones.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            // Esto copiará el archivo de resources a la carpeta del plugin
            plugin.saveResource("colecciones.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void guardarConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("¡No se pudo guardar el archivo colecciones.yml!");
        }
    }

    public void recargarConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // ==========================================================
    // 🔍 MÉTODOS DE LECTURA
    // ==========================================================

    public boolean esColeccion(String id) {
        return config.contains("colecciones." + id);
    }

    public ConfigurationSection getDatosColeccion(String id) {
        return config.getConfigurationSection("colecciones." + id);
    }

    public boolean esSlayer(String id) {
        return config.contains("slayers." + id);
    }

    public ConfigurationSection getDatosSlayer(String id) {
        return config.getConfigurationSection("slayers." + id);
    }
}