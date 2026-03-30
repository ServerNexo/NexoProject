package me.nexo.colecciones;

import me.nexo.colecciones.api.ColeccionesExpansion;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoColeccionesTabCompleter;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.user.NexoAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoColecciones extends JavaPlugin {

    private CollectionManager collectionManager;
    private ColeccionesConfig coleccionesConfig;
    private SlayerManager slayerManager;

    @Override
    public void onEnable() {
        this.coleccionesConfig = new ColeccionesConfig(this);
        this.collectionManager = new CollectionManager(this);
        this.slayerManager = new SlayerManager(this);

        NexoAPI.getServices().register(CollectionManager.class, this.collectionManager);
        NexoAPI.getServices().register(SlayerManager.class, this.slayerManager);

        coleccionesConfig.recargarConfig();
        collectionManager.cargarDesdeConfig();
        slayerManager.cargarSlayers();

        getServer().getPluginManager().registerEvents(new ColeccionesListener(this), this);
        getServer().getPluginManager().registerEvents(new SlayerListener(this), this);


        if (getCommand("colecciones") != null) {
            getCommand("colecciones").setExecutor(new ComandoColecciones(this));
            getCommand("colecciones").setTabCompleter(new ComandoColeccionesTabCompleter());
        }
        if (getCommand("slayer") != null) getCommand("slayer").setExecutor(new ComandoSlayer(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ColeccionesExpansion(this).register();
        }

        new FlushTask(this).runTaskTimerAsynchronously(this, 20L * 60 * 5, 20L * 60 * 5);

        getLogger().info("NexoColecciones ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        new FlushTask(this).run();
        NexoAPI.getServices().unregister(CollectionManager.class);
        NexoAPI.getServices().unregister(SlayerManager.class);
        getLogger().info("NexoColecciones ha sido deshabilitado.");
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public ColeccionesConfig getColeccionesConfig() {
        return coleccionesConfig;
    }

    public SlayerManager getSlayerManager() {
        return slayerManager;
    }
}