package me.nexo.items;

import me.nexo.core.user.NexoAPI;
import me.nexo.items.accesorios.AccesoriosListener;
import me.nexo.items.accesorios.AccesoriosManager;
import me.nexo.items.accesorios.ComandoAccesorios;
import me.nexo.items.artefactos.ArtefactoListener;
import me.nexo.items.artefactos.ArtefactoManager;
import me.nexo.items.commands.ComandoDesguaceTabCompleter;
import me.nexo.items.config.ConfigManager;
import me.nexo.items.estaciones.*;
import me.nexo.items.guardarropa.ComandoWardrobe;
import me.nexo.items.guardarropa.GuardarropaListener;
import me.nexo.items.guardarropa.GuardarropaManager;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import me.nexo.items.mochilas.ComandoPV;
import me.nexo.items.mochilas.MochilaListener;
import me.nexo.items.mochilas.MochilaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoItems extends JavaPlugin {

    private FileManager fileManager;
    private AccesoriosManager accesoriosManager;
    private ArtefactoManager artefactoManager;
    private GuardarropaManager guardarropaManager;
    private MochilaManager mochilaManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🗡️ NexoItems activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        this.configManager = new ConfigManager(this);
        this.fileManager = new FileManager(this);
        ItemManager.init(this);

        this.accesoriosManager = new AccesoriosManager(this);
        this.artefactoManager = new ArtefactoManager(this);
        this.guardarropaManager = new GuardarropaManager(this);
        this.mochilaManager = new MochilaManager(this);

        NexoAPI.getServices().register(ItemManager.class, new ItemManager());

        GuardarropaListener wardrobeListener = new GuardarropaListener(this);

        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new DesguaceListener(this), this);
        getServer().getPluginManager().registerEvents(new HerreriaListener(this), this);
        getServer().getPluginManager().registerEvents(new ReforjaListener(this), this);
        getServer().getPluginManager().registerEvents(new YunqueListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerItemListener(this), this);
        getServer().getPluginManager().registerEvents(new VanillaStationsListener(this), this);
        getServer().getPluginManager().registerEvents(new AccesoriosListener(this, this.accesoriosManager), this);
        getServer().getPluginManager().registerEvents(new ArtefactoListener(this, this.artefactoManager), this);
        getServer().getPluginManager().registerEvents(wardrobeListener, this);
        getServer().getPluginManager().registerEvents(new MochilaListener(this.mochilaManager), this);

        if (getCommand("desguace") != null) {
            getCommand("desguace").setExecutor(new ComandoDesguace(this));
            getCommand("desguace").setTabCompleter(new ComandoDesguaceTabCompleter());
        }
        if (getCommand("accesorios") != null) getCommand("accesorios").setExecutor(new ComandoAccesorios(this));
        if (getCommand("wardrobe") != null) getCommand("wardrobe").setExecutor(new ComandoWardrobe(this, wardrobeListener));
        if (getCommand("pv") != null) getCommand("pv").setExecutor(new ComandoPV(this));
    }

    @Override
    public void onDisable() {
        BlockBreakListener.restaurarBloquesRotos();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Aquí irían las llamadas a los métodos de guardado síncrono
        }
        NexoAPI.getServices().unregister(ItemManager.class);
        getLogger().info("🗡️ NexoItems apagado y desconectado.");
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public AccesoriosManager getAccesoriosManager() {
        return accesoriosManager;
    }

    public ArtefactoManager getArtefactoManager() {
        return artefactoManager;
    }

    public GuardarropaManager getGuardarropaManager() {
        return guardarropaManager;
    }

    public MochilaManager getMochilaManager() {
        return mochilaManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}