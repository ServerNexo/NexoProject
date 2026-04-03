package me.nexo.core.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.core.DatabaseManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.api.NexoWebServer;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.api.ServiceBootstrap;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

public class NexoCoreModule extends AbstractModule {

    private final NexoCore plugin;

    public NexoCoreModule(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos las instancias de Paper API
        bind(Plugin.class).toInstance(plugin);
        bind(NexoCore.class).toInstance(plugin);
        bind(Server.class).toInstance(plugin.getServer());

        // 💉 Asignamos los Managers como Singletons (Patrón Singleton gestionado por DI)
        bind(ConfigManager.class).asEagerSingleton();
        bind(DatabaseManager.class).asEagerSingleton();
        bind(UserManager.class).asEagerSingleton();
        bind(NexoWebServer.class).asEagerSingleton();
        bind(ServiceBootstrap.class).asEagerSingleton();
    }
}