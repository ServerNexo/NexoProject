package me.nexo.clans;

import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

// 🌟 Importamos nuestras nuevas clases
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.commands.ComandoClan;

public class NexoClans extends JavaPlugin {

    // 🌟 1. Declaramos el Manager para que viva en la memoria
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Iniciando NexoClans...");

        // Verificamos que NexoCore esté funcionando
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        if (core == null || core.getDatabaseManager() == null) {
            getLogger().severe("❌ NexoCore no detectado o Base de Datos inactiva. Apagando NexoClans...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("🔗 Conectado al motor de base de datos de NexoCore.");

        // 🌟 2. Inicializamos el Caché de Clanes
        this.clanManager = new ClanManager(this);

        // 🌟 3. Registramos los Listeners y Comandos
        getServer().getPluginManager().registerEvents(new ClanConnectionListener(this), this);

        if (getCommand("clan") != null) {
            getCommand("clan").setExecutor(new ComandoClan(this));
        }

        getLogger().info("✅ ¡NexoClans cargado exitosamente!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        // Aquí forzaremos el guardado asíncrono de los clanes al apagar el servidor
        getLogger().info("💾 Guardando datos de clanes y apagando...");
    }

    // 🌟 4. Getter para que otros archivos puedan usar la memoria RAM de clanes
    public ClanManager getClanManager() {
        return clanManager;
    }
}