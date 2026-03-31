package me.nexo.dungeons;

import me.nexo.core.user.NexoAPI;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.commands.ComandoDungeonTabCompleter;
import me.nexo.dungeons.config.ConfigManager; // 🌟 IMPORTACIÓN AÑADIDA
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoDungeons extends JavaPlugin {

    // 🌟 SE AÑADIÓ EL CONFIG MANAGER
    private ConfigManager configManager;
    private WaveManager waveManager;
    private QueueManager queueManager;
    private PuzzleEngine puzzleEngine;

    @Override
    public void onEnable() {
        // 🌟 SE INICIALIZA EL CONFIG MANAGER
        this.configManager = new ConfigManager(this);

        this.waveManager = new WaveManager(this);
        this.queueManager = new QueueManager(this);
        this.puzzleEngine = new PuzzleEngine(this);

        NexoAPI.getServices().register(WaveManager.class, this.waveManager);
        NexoAPI.getServices().register(QueueManager.class, this.queueManager);
        NexoAPI.getServices().register(PuzzleEngine.class, this.puzzleEngine);

        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonSecurityListener(this), this);
        getServer().getPluginManager().registerEvents(new LootProtectionListener(this), this);

        // ✂️ SE ELIMINÓ LA LÍNEA DEL DungeonMenuListener QUE CAUSABA ERROR

        if (getCommand("dungeons") != null) {
            getCommand("dungeons").setExecutor(new ComandoDungeon(this));
            getCommand("dungeons").setTabCompleter(new ComandoDungeonTabCompleter());
        }

        getLogger().info("NexoDungeons ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        NexoAPI.getServices().unregister(WaveManager.class);
        NexoAPI.getServices().unregister(QueueManager.class);
        NexoAPI.getServices().unregister(PuzzleEngine.class);
        getLogger().info("NexoDungeons ha sido deshabilitado.");
    }

    // 🌟 SE AÑADIÓ EL GETTER PARA QUE DUNGEONMENU PUEDA LEER LOS MENSAJES
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public PuzzleEngine getPuzzleEngine() {
        return puzzleEngine;
    }
}