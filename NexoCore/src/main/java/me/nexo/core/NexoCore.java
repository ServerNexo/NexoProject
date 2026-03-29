package me.nexo.core;

import me.nexo.core.api.NexoWebServer;
import me.nexo.core.commands.ComandoNexo;
import me.nexo.core.commands.ComandoNexoTabCompleter;
import me.nexo.core.commands.WebCommand;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.hub.NexoMenuListener;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.UserManager;
import me.nexo.core.listeners.VoidEssenceListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private UserManager userManager;
    private NexoAPI nexoAPI;
    private ConfigManager configManager;
    private NexoWebServer webServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();

        this.userManager = new UserManager();
        this.nexoAPI = new NexoAPI(this.userManager);

        this.webServer = new NexoWebServer(this);
        this.webServer.start();

        getCommand("web").setExecutor(new WebCommand(this));
        if (getCommand("nexocore") != null) {
            getCommand("nexocore").setExecutor(new ComandoNexo(this));
            getCommand("nexocore").setTabCompleter(new ComandoNexoTabCompleter());
        }
        if (getCommand("void") != null) {
            getCommand("void").setExecutor(new me.nexo.core.commands.ComandoVoid(this));
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VoidEssenceListener(this), this);
        getServer().getPluginManager().registerEvents(new NexoMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new me.nexo.core.menus.VoidBlessingMenuListener(), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        new HudTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (databaseManager != null) {
                databaseManager.guardarJugadorSync(p);
            }
        }
        if (databaseManager != null) databaseManager.desconectar();
        getLogger().info("NexoCore apagado y datos guardados.");
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public UserManager getUserManager() { return userManager; }
    public ConfigManager getConfigManager() { return configManager; }
}