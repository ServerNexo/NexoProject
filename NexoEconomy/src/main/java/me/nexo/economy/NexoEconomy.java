package me.nexo.economy;

import me.nexo.core.NexoCore;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.commands.ComandoBazar;
import me.nexo.economy.commands.ComandoEco;
import me.nexo.economy.commands.ComandoTrade;
import me.nexo.economy.commands.ComandoMercadoNegro; // 🌑 NUEVO IMPORT
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.listeners.EconomyListener;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoEconomy extends JavaPlugin {

    private EconomyManager economyManager;
    private TradeManager tradeManager;
    private BazaarManager bazaarManager;
    private BlackMarketManager blackMarketManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🪙 Iniciando NexoEconomy...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 INICIALIZAMOS LOS MOTORES EN ORDEN
        this.economyManager = new EconomyManager(this);
        this.tradeManager = new TradeManager(this);
        this.bazaarManager = new BazaarManager(this);
        this.blackMarketManager = new BlackMarketManager(this);

        // Registramos Listeners
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        getServer().getPluginManager().registerEvents(new me.nexo.economy.listeners.TradeListener(this), this);
        getServer().getPluginManager().registerEvents(new me.nexo.economy.blackmarket.BlackMarketListener(this), this); // 🌑 LISTENER MERCADO NEGRO
        getServer().getPluginManager().registerEvents(new me.nexo.economy.bazar.BazaarMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new me.nexo.economy.bazar.BazaarChatListener(this), this);

        // Registramos Comandos
        if (getCommand("eco") != null) {
            getCommand("eco").setExecutor(new ComandoEco(this));
        }

        if (getCommand("trade") != null) {
            getCommand("trade").setExecutor(new ComandoTrade(this));
        }

        if (getCommand("bazar") != null) {
            getCommand("bazar").setExecutor(new ComandoBazar(this));
        }

        // 🌑 COMANDO MERCADO NEGRO
        if (getCommand("mercadonegro") != null) {
            getCommand("mercadonegro").setExecutor(new ComandoMercadoNegro(this));
        }

        getLogger().info("✅ ¡NexoEconomy cargado y operativo!");
        getLogger().info("========================================");
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
}