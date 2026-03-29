package me.nexo.dungeons;

import me.nexo.core.user.NexoAPI;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.commands.ComandoDungeonTabCompleter;
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

    @Override
    public void onEnable() {
        this.waveManager = new WaveManager(this);
        this.queueManager = new QueueManager(this);

        NexoAPI.getServices().register(WaveManager.class, this.waveManager);
        NexoAPI.getServices().register(QueueManager.class, this.queueManager);

        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonSecurityListener(), this);
        getServer().getPluginManager().registerEvents(new LootProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new DungeonMenuListener(this), this);

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
        getLogger().info("NexoDungeons ha sido deshabilitado.");
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
}