package me.nexo.pvp;

import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.pvp.pvp.PvPManager;
import me.nexo.pvp.pvp.PvPListener;
import me.nexo.pvp.pvp.ComandoPvP;

// Importamos las pasivas
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pasivas.PasivasListener;

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

        if (getCommand("pvp") != null) {
            getCommand("pvp").setExecutor(new ComandoPvP(this.pvpManager));
        }

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚔️ NexoPvP apagado.");
    }
}