package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoCore extends JavaPlugin {

    private DatabaseManager databaseManager;

    private UserManager userManager;
    private NexoAPI nexoAPI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 🟢 Solo nos conectamos a la BD
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();

        this.userManager = new UserManager();
        this.nexoAPI = new NexoAPI(this.userManager);

        // 🟢 Solo conservamos el comando del Core
        if (getCommand("nexocore") != null) getCommand("nexocore").setExecutor(new ComandoNexo(this));

        // 🟢 Listeners generales (¡El único que sobrevive aquí es el de la Base de Datos!)
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // Tarea del HUD
        new HudTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("¡Nexo Core V8.2: Core Purificado al 100%!");
    }

    @Override
    public void onDisable() {
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