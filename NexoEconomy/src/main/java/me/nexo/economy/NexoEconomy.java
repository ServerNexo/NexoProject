package me.nexo.economy;

import me.nexo.core.NexoCore;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.blackmarket.BlackMarketManager; // 🌑 NUEVO IMPORT
import me.nexo.economy.commands.ComandoBazar;
import me.nexo.economy.commands.ComandoEco;
import me.nexo.economy.commands.ComandoTrade;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.listeners.EconomyListener;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoEconomy extends JavaPlugin {

    private EconomyManager economyManager;
    private TradeManager tradeManager;
    private BazaarManager bazaarManager;
    private BlackMarketManager blackMarketManager; // 🌑 NUEVA VARIABLE

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
        this.blackMarketManager = new BlackMarketManager(this); // 🌑 Levantamos las sombras

        // Registramos Listeners
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        getServer().getPluginManager().registerEvents(new me.nexo.economy.listeners.TradeListener(this), this);

        // Registramos Comandos
        if (getCommand("eco") != null) {
            getCommand("eco").setExecutor(new ComandoEco(this));
        }

        if (getCommand("trade") != null) {
            getCommand("trade").setExecutor(new ComandoTrade(this));
        }

        // 📈 Registramos el Bazar
        if (getCommand("bazar") != null) {
            getCommand("bazar").setExecutor(new ComandoBazar(this));
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

    // 🌑 NUEVO GETTER
    public BlackMarketManager getBlackMarketManager() {
        return blackMarketManager;
    }
}