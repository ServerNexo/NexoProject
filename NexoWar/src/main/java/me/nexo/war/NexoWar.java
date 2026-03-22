package me.nexo.war;

import me.nexo.war.commands.ComandoWar;
import me.nexo.war.listeners.WarListener;
import me.nexo.war.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoWar extends JavaPlugin {

    private WarManager warManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ Iniciando NexoWar (Sistema de Guerras y Apuestas)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null ||
                getServer().getPluginManager().getPlugin("NexoClans") == null ||
                getServer().getPluginManager().getPlugin("NexoEconomy") == null ||
                getServer().getPluginManager().getPlugin("NexoProtections") == null) {

            getLogger().severe("❌ Error Crítico: Faltan dependencias nativas del ecosistema Nexo.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.warManager = new WarManager(this);

        if (getCommand("war") != null) {
            getCommand("war").setExecutor(new ComandoWar(this));
        }

        // 🌟 Registramos el Rastreador de Muertes
        getServer().getPluginManager().registerEvents(new WarListener(this), this);

        getLogger().info("✅ ¡NexoWar cargado exitosamente! Los clanes están listos para la batalla.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚔️ NexoWar apagado. El estado de las guerras activas está a salvo en la base de datos.");
    }

    public WarManager getWarManager() {
        return warManager;
    }
}