package me.nexo.pvp;

import me.nexo.pvp.classes.ArmorClassListener;
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

    private PvPManager pvpManager;
    private PasivasManager pasivasManager; // 🟢 Añadimos esta variable

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ NexoPvP activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // ==========================================
        // 1. INICIALIZAMOS LOS MANAGERS
        // ==========================================
        this.pvpManager = new PvPManager(this);
        this.pasivasManager = new PasivasManager(this); // 🟢 Inicializamos Pasivas

        // ==========================================
        // 2. REGISTRAMOS EVENTOS Y COMANDOS
        // ==========================================
        getServer().getPluginManager().registerEvents(new PvPListener(this.pvpManager), this);

        // 🟢 Registramos el Listener de las pasivas
        getServer().getPluginManager().registerEvents(new PasivasListener(this, this.pasivasManager), this);
        getServer().getPluginManager().registerEvents(new ArmorClassListener(this), this);

        // 🩸 Registramos la Penalización de Muerte Hardcore (Filosofía Tibia)
        getServer().getPluginManager().registerEvents(new DeathPenaltyListener(), this);

        if (getCommand("pvp") != null) {
            getCommand("pvp").setExecutor(new ComandoPvP(this.pvpManager));
        }

        // Registrar el Listener del Menú
        getServer().getPluginManager().registerEvents(new me.nexo.pvp.menus.BlessingMenuListener(), this);

        // Registrar el comando /templo
        if (getCommand("templo") != null) {
            getCommand("templo").setExecutor(new me.nexo.pvp.commands.ComandoTemplo());
        }

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚔️ NexoPvP apagado.");
    }
}