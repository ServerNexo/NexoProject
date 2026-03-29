package me.nexo.minions;

import me.nexo.core.user.NexoAPI;
import me.nexo.minions.commands.ComandoMinion;
import me.nexo.minions.commands.ComandoMinionTabCompleter;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.listeners.ExplosionListener;
import me.nexo.minions.listeners.MenuListener;
import me.nexo.minions.listeners.MinionInteractListener;
import me.nexo.minions.listeners.MinionLoadListener;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMinions extends JavaPlugin {

    private MinionManager minionManager;
    private TiersConfig tiersConfig;
    private UpgradesConfig upgradesConfig;

    @Override
    public void onEnable() {
        this.minionManager = new MinionManager(this);
        this.tiersConfig = new TiersConfig(this);
        this.upgradesConfig = new UpgradesConfig(this);

        NexoAPI.getServices().register(MinionManager.class, this.minionManager);

        getServer().getPluginManager().registerEvents(new MinionInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new MinionLoadListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        // 🌟 CORRECCIÓN: Le pasamos 'this' al ExplosionListener
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);

        if (getCommand("minion") != null) {
            getCommand("minion").setExecutor(new ComandoMinion(this));
            getCommand("minion").setTabCompleter(new ComandoMinionTabCompleter());
        }

        getServer().getScheduler().runTaskTimer(this, () -> minionManager.tickAll(System.currentTimeMillis()), 20L, 20L);

        getLogger().info("NexoMinions ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        if (minionManager != null) {
            minionManager.saveAllMinionsSync();
        }
        NexoAPI.getServices().unregister(MinionManager.class);
        getLogger().info("NexoMinions ha sido deshabilitado.");
    }

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public TiersConfig getTiersConfig() {
        return tiersConfig;
    }

    public UpgradesConfig getUpgradesConfig() {
        return upgradesConfig;
    }
}