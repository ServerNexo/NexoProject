package me.nexo.dungeons;

import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.menu.DungeonMenuListener;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoDungeons extends JavaPlugin {

    private WaveManager waveManager;
    private QueueManager queueManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.waveManager = new WaveManager(this);
        this.queueManager = new QueueManager(this);

        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonSecurityListener(), this);
        getServer().getPluginManager().registerEvents(new LootProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonMenuListener(this), this);

        if (getCommand("dungeons") != null) {
            getCommand("dungeons").setExecutor(new me.nexo.dungeons.commands.ComandoDungeon(this));
        }

        getLogger().info("NexoDungeons ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexoDungeons ha sido deshabilitado.");
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}