package me.nexo.clans;

import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;
import me.nexo.clans.menu.ClanMenuListener;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoClans extends JavaPlugin {

    private ClanManager clanManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.clanManager = new ClanManager(this);

        getServer().getPluginManager().registerEvents(new ClanConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanMenuListener(this), this);

        if (getCommand("clan") != null) getCommand("clan").setExecutor(new ComandoClan(this));
        if (getCommand("c") != null) getCommand("c").setExecutor(new ComandoChatClan(this));

        getLogger().info("NexoClans ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexoClans ha sido deshabilitado.");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}