package me.nexo.minions;

import me.nexo.minions.commands.ComandoMinion;
import me.nexo.minions.data.TiersConfig; // 🌟 El nuevo import
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.listeners.MenuListener;
import me.nexo.minions.listeners.MinionInteractListener;
import me.nexo.minions.listeners.MinionListener;
import me.nexo.minions.listeners.MinionLoadListener;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMinions extends JavaPlugin {

    private MinionManager minionManager;
    private UpgradesConfig upgradesConfig;
    private TiersConfig tiersConfig; // 🌟 La nueva variable

    @Override
    public void onEnable() {
        // 1. Cargamos los archivos de configuración
        this.upgradesConfig = new UpgradesConfig(this);
        this.tiersConfig = new TiersConfig(this); // 🌟 Inicializamos los costos

        // 2. Inicializamos el cerebro de las abejas
        this.minionManager = new MinionManager(this);

        // 3. Registramos el comando
        if (getCommand("minion") != null) {
            getCommand("minion").setExecutor(new ComandoMinion(this));
        }

        // 4. Registramos los eventos
        getServer().getPluginManager().registerEvents(new MinionListener(this), this);
        getServer().getPluginManager().registerEvents(new MinionInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MinionLoadListener(this), this);

        // 5. El Reloj de las Abejas (Tick)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (minionManager != null) {
                minionManager.tickAll(System.currentTimeMillis());
            }
        }, 20L, 20L);

        getLogger().info("========================================");
        getLogger().info("🐝 NexoMinions V1.0 - ¡Sistema de Abejas Activado!");
        getLogger().info("⚙️ Upgrades y Costos de Evolución cargados.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.minionManager != null) {
            getLogger().info("NexoMinions ha sido desactivado. ¡Abejas a dormir!");
        }
    }

    // ==========================================
    // GETTERS GLOBALES
    // ==========================================
    public MinionManager getMinionManager() {
        return minionManager;
    }

    public UpgradesConfig getUpgradesConfig() {
        return upgradesConfig;
    }

    // 🌟 El getter que le faltaba a tu código para borrar ese error rojo
    public TiersConfig getTiersConfig() {
        return tiersConfig;
    }
}