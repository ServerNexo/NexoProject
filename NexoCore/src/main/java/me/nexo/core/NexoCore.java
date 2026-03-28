package me.nexo.core;

import me.nexo.core.api.NexoWebServer; // 🌟 IMPORT NUEVO
import me.nexo.core.hub.NexoMenuListener; // 🌟 IMPORT NUEVO
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

    private NexoWebServer webServer; // 🌟 NUEVO: Variable del Servidor Web

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 🟢 Solo nos conectamos a la BD
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();

        this.userManager = new UserManager();
        this.nexoAPI = new NexoAPI(this.userManager);

        // 🌟 NUEVO: Arrancamos la NexoWeb API (Hilos Virtuales)
        this.webServer = new NexoWebServer(this);
        this.webServer.start();

        // 🟢 Solo conservamos el comando del Core
        if (getCommand("nexocore") != null) getCommand("nexocore").setExecutor(new ComandoNexo(this));
        // Registrar el comando
        if (getCommand("void") != null) {
            getCommand("void").setExecutor(new me.nexo.core.commands.ComandoVoid(this));
        }

        // 🟢 Listeners generales
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VoidEssenceListener(this), this);

        // 🌟 NUEVO: Registramos la protección del Ítem del Hub (Slot 9)
        getServer().getPluginManager().registerEvents(new NexoMenuListener(this), this);
        // Registrar el Listener de protección del menú
        getServer().getPluginManager().registerEvents(new me.nexo.core.menus.VoidBlessingMenuListener(), this);


        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // Tarea del HUD
        new HudTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    @Override
    public void onDisable() {
        // 🌟 NUEVO: Apagamos el Servidor Web de forma segura
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
}