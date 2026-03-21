package me.nexo.economy;

import me.nexo.core.NexoCore;
import me.nexo.economy.commands.ComandoEco;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.listeners.EconomyListener;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoEconomy extends JavaPlugin {

    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🪙 Iniciando NexoEconomy...");

        // Verificación de seguridad del Core
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando NexoEconomy...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializamos el motor de base de datos financiero
        this.economyManager = new EconomyManager(this);

        // Registramos eventos y comandos
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);

        if (getCommand("eco") != null) {
            getCommand("eco").setExecutor(new ComandoEco(this));
        }

        getLogger().info("✅ ¡NexoEconomy cargado y operativo!");
        getLogger().info("========================================");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}