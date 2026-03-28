package me.nexo.economy;

import me.nexo.economy.bazar.BazaarChatListener;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.bazar.BazaarMenuListener;
import me.nexo.economy.blackmarket.BlackMarketListener;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.commands.ComandoBazar;
import me.nexo.economy.commands.ComandoEco;
import me.nexo.economy.commands.ComandoMercadoNegro;
import me.nexo.economy.commands.ComandoTrade;
import me.nexo.economy.config.ConfigManager;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.listeners.EconomyListener;
import me.nexo.economy.listeners.TradeListener;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoEconomy extends JavaPlugin {

    private EconomyManager economyManager;
    private TradeManager tradeManager;
    private BazaarManager bazaarManager;
    private BlackMarketManager blackMarketManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager(this);
        this.tradeManager = new TradeManager(this);
        this.bazaarManager = new BazaarManager(this);
        this.blackMarketManager = new BlackMarketManager(this);

        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeListener(this), this);
        getServer().getPluginManager().registerEvents(new BazaarChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BazaarMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new BlackMarketListener(this), this);

        if (getCommand("eco") != null) getCommand("eco").setExecutor(new ComandoEco(this));
        if (getCommand("trade") != null) getCommand("trade").setExecutor(new ComandoTrade(this));
        if (getCommand("bazar") != null) getCommand("bazar").setExecutor(new ComandoBazar(this));
        if (getCommand("mercadonegro") != null) getCommand("mercadonegro").setExecutor(new ComandoMercadoNegro(this));

        getLogger().info("NexoEconomy ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexoEconomy ha sido deshabilitado.");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public BazaarManager getBazaarManager() {
        return bazaarManager;
    }

    public BlackMarketManager getBlackMarketManager() {
        return blackMarketManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}