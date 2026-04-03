package me.nexo.core.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import me.nexo.core.NexoCore;
import me.nexo.core.DatabaseManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.PlayerListener;
import me.nexo.core.HudTask;
import me.nexo.core.NexoExpansion;
import me.nexo.core.commands.ComandoNexo;
import me.nexo.core.commands.ComandoVoid;
// Si WebCommand aún no está refactorizado, lo dejamos comentado por ahora.

import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.logging.Logger;

@Singleton
public class ServiceBootstrap {

    private final NexoCore plugin;
    private final Server server;
    private final Logger logger;
    private final Injector injector; // 💉 Inyectamos a Guice mismo

    // Dependencias inyectadas automáticamente
    private final DatabaseManager databaseManager;
    private final UserManager userManager;
    private final NexoWebServer webServer;
    private final ConfigManager configManager;

    @Inject
    public ServiceBootstrap(NexoCore plugin, Server server, Injector injector,
                            DatabaseManager databaseManager, UserManager userManager,
                            NexoWebServer webServer, ConfigManager configManager) {
        this.plugin = plugin;
        this.server = server;
        this.logger = plugin.getLogger();
        this.injector = injector;
        this.databaseManager = databaseManager;
        this.userManager = userManager;
        this.webServer = webServer;
        this.configManager = configManager;
    }

    public void startServices() {
        logger.info("========================================");
        logger.info("⚡ Arrancando Arquitectura Nexo Enterprise");
        logger.info("========================================");

        // 1. Inicializar Bases de Datos
        databaseManager.conectar();

        // 2. Iniciar API Web
        webServer.start();

        // 3. Registrar Eventos
        registerEvents();

        // 4. Tareas en Segundo Plano
        new HudTask(plugin).runTaskTimer(plugin, 20L, 20L);

        // 5. Hooks Externos
        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(plugin).register();
        }

        // 6. 💡 PILAR 1: Registro de comandos moderno (Lamp)
        registerCommands();

        logger.info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    public void stopServices() {
        if (webServer != null) {
            webServer.stop();
        }

        // 🗄️ PILAR 4: Esto debe migrar a un UserRepository en el futuro
        for (Player p : server.getOnlinePlayers()) {
            if (databaseManager != null) {
                databaseManager.guardarJugadorSync(p);
            }
        }

        if (databaseManager != null) {
            databaseManager.desconectar();
        }

        logger.info("NexoCore apagado y datos guardados de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // Nota: En una fase posterior, inyectaremos también los Listeners para que reciban los Managers automáticamente.
        pm.registerEvents(new PlayerListener(plugin), plugin);
        pm.registerEvents(new me.nexo.core.listeners.VoidEssenceListener(plugin), plugin);
        pm.registerEvents(new me.nexo.core.hub.NexoMenuListener(plugin), plugin);
        pm.registerEvents(new me.nexo.core.menus.VoidBlessingMenuListener(), plugin);
        pm.registerEvents(new me.nexo.core.menus.MenuGlobalListener(), plugin);
    }

    private void registerCommands() {
        // 1. Inicializamos el framework de Lamp
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 2. Le pedimos a Guice que nos construya los comandos con sus dependencias inyectadas
        handler.register(injector.getInstance(ComandoNexo.class));
        handler.register(injector.getInstance(ComandoVoid.class));

        // Cuando refactoricemos WebCommand, lo descomentas:
        // handler.register(injector.getInstance(me.nexo.core.commands.WebCommand.class));
    }
}