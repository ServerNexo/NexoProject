package me.nexo.pvp;

import me.nexo.pvp.classes.ArmorClassListener;
import me.nexo.pvp.commands.ComandoPvPTabCompleter;
import me.nexo.pvp.config.ConfigManager; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.pvp.pvp.PvPManager;
import me.nexo.pvp.pvp.PvPListener;
import me.nexo.pvp.pvp.ComandoPvP;

// Importamos las pasivas
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pasivas.PasivasListener;

// 🩸 Módulo de Mecánicas Hardcore (Nexo Architect V3.0)
import me.nexo.pvp.mechanics.DeathPenaltyListener;

public class NexoPvP extends JavaPlugin {

    // 🌟 DECLARACIÓN DEL CONFIG MANAGER
    private ConfigManager configManager;

    private PvPManager pvpManager;
    private PasivasManager pasivasManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ NexoPvP activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // 🌟 INICIALIZAR EL CONFIG MANAGER PRIMERO
        this.configManager = new ConfigManager(this);

        this.pvpManager = new PvPManager(this);
        this.pasivasManager = new PasivasManager(this);

        getServer().getPluginManager().registerEvents(new PvPListener(this.pvpManager), this);
        getServer().getPluginManager().registerEvents(new PasivasListener(this, this.pasivasManager), this);
        getServer().getPluginManager().registerEvents(new ArmorClassListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathPenaltyListener(), this);

        if (getCommand("pvp") != null) {
            getCommand("pvp").setExecutor(new ComandoPvP(this.pvpManager));
            getCommand("pvp").setTabCompleter(new ComandoPvPTabCompleter());
        }

        if (getCommand("templo") != null) {
            getCommand("templo").setExecutor(new me.nexo.pvp.commands.ComandoTemplo());
        }

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚔️ NexoPvP apagado.");
    }

    // 🌟 GETTER AÑADIDO PARA LOS MENÚS Y EVENTOS
    public ConfigManager getConfigManager() {
        return configManager;
    }
}