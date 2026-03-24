package me.nexo.items;

import me.nexo.items.estaciones.DesguaceListener;
import me.nexo.items.estaciones.HerreriaListener;
import me.nexo.items.estaciones.ReforjaListener;
import me.nexo.items.estaciones.YunqueListener;
import me.nexo.items.estaciones.UpgradeListener;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.items.accesorios.AccesoriosManager;
import me.nexo.items.accesorios.AccesoriosListener;
import me.nexo.items.accesorios.ComandoAccesorios;

import me.nexo.items.artefactos.ArtefactoManager;
import me.nexo.items.artefactos.ArtefactoListener;

import me.nexo.items.guardarropa.GuardarropaManager;
import me.nexo.items.guardarropa.GuardarropaListener;
import me.nexo.items.guardarropa.ComandoWardrobe;

import me.nexo.items.mochilas.MochilaManager;
import me.nexo.items.mochilas.MochilaListener;
import me.nexo.items.mochilas.ComandoPV;

public class NexoItems extends JavaPlugin {

    // 🌟 DECLARACIÓN GLOBAL DE MANAGERS
    private FileManager fileManager;
    private AccesoriosManager accesoriosManager;
    private ArtefactoManager artefactoManager;
    private GuardarropaManager guardarropaManager;
    private MochilaManager mochilaManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🗡️ NexoItems activado correctamente.");
        getLogger().info("🔗 Conectado al cerebro: NexoCore.");

        // ==========================================
        // 1. INICIALIZACIÓN DE MOTORES BASE
        // ==========================================
        this.fileManager = new FileManager(this);
        ItemManager.init(this);

        // ==========================================
        // 2. INICIALIZACIÓN DE MANAGERS (Sub-sistemas)
        // ==========================================
        this.accesoriosManager = new AccesoriosManager(this);
        this.artefactoManager = new ArtefactoManager(this);
        this.guardarropaManager = new GuardarropaManager(this);
        this.mochilaManager = new MochilaManager(this);

        GuardarropaListener wardrobeListener = new GuardarropaListener(this.guardarropaManager);

        // ==========================================
        // 3. REGISTRO DE EVENTOS (Listeners)
        // ==========================================
        // Sistemas Base
        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new DesguaceListener(this), this);
        getServer().getPluginManager().registerEvents(new HerreriaListener(this), this);
        getServer().getPluginManager().registerEvents(new ReforjaListener(this), this);
        getServer().getPluginManager().registerEvents(new YunqueListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);

        // 🌟 NUEVO SISTEMA: MESA DE EVOLUCIÓN CÉNIT (FASE 5)
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);

        // Sistemas Mudados del Core
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerItemListener(this), this);
        getServer().getPluginManager().registerEvents(new VanillaStationsListener(this), this);

        // Nuevos Sistemas Modulares
        getServer().getPluginManager().registerEvents(new AccesoriosListener(this, this.accesoriosManager), this);
        getServer().getPluginManager().registerEvents(new ArtefactoListener(this, this.artefactoManager), this);
        getServer().getPluginManager().registerEvents(wardrobeListener, this);
        getServer().getPluginManager().registerEvents(new MochilaListener(this.mochilaManager), this);

        // ==========================================
        // 4. REGISTRO DE COMANDOS
        // ==========================================
        if (getCommand("desguace") != null) getCommand("desguace").setExecutor(new ComandoDesguace());
        if (getCommand("accesorios") != null) getCommand("accesorios").setExecutor(new ComandoAccesorios(this));

        // 🌟 CORRECCIÓN: Le pasamos el 'wardrobeListener' en lugar de 'this'
        if (getCommand("wardrobe") != null) getCommand("wardrobe").setExecutor(new ComandoWardrobe(wardrobeListener));

        if (getCommand("pv") != null) getCommand("pv").setExecutor(new ComandoPV(this));
    }

    @Override
    public void onDisable() {
        BlockBreakListener.restaurarBloquesRotos();
        getLogger().info("🗡️ NexoItems apagado y desconectado.");
    }

    // ==========================================
    // 🌟 GETTERS DE MANAGERS (Faltaban estos)
    // ==========================================
    public FileManager getFileManager() { return fileManager; }
    public AccesoriosManager getAccesoriosManager() { return accesoriosManager; }
    public ArtefactoManager getArtefactoManager() { return artefactoManager; }
    public GuardarropaManager getGuardarropaManager() { return guardarropaManager; }
    public MochilaManager getMochilaManager() { return mochilaManager; }
}