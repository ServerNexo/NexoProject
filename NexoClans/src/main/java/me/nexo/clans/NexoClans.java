package me.nexo.clans;

import me.nexo.clans.core.ClanManager;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.commands.ComandoClanTabCompleter;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;
import me.nexo.clans.menu.ClanMenuListener;
import me.nexo.core.user.NexoAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoClans extends JavaPlugin {

    private ClanManager clanManager;

    @Override
    public void onEnable() {
        this.clanManager = new ClanManager(this);

        NexoAPI.getServices().register(ClanManager.class, this.clanManager);

        getServer().getPluginManager().registerEvents(new ClanConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanMenuListener(this), this);

        if (getCommand("clan") != null) {
            getCommand("clan").setExecutor(new ComandoClan(this));
            getCommand("clan").setTabCompleter(new ComandoClanTabCompleter());
        }
        if (getCommand("c") != null) getCommand("c").setExecutor(new ComandoChatClan(this));

        getLogger().info("NexoClans ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Guardando datos de clanes...");
        if (clanManager != null) {
            clanManager.saveAllClansSync();
        }
        NexoAPI.getServices().unregister(ClanManager.class);
        getLogger().info("NexoClans ha sido deshabilitado.");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }
}