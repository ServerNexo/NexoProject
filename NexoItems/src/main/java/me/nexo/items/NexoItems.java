package me.nexo.items;

import me.nexo.items.estaciones.DesguaceListener;
import me.nexo.items.estaciones.HerreriaListener;
import me.nexo.items.estaciones.ReforjaListener;
import me.nexo.items.estaciones.YunqueListener;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import org.bukkit.plugin.java.JavaPlugin;

// 🟢 Importamos todos tus nuevos sistemas purificados
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

    // 🟢 Agregamos el FileManager
    private FileManager fileManager;

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
        AccesoriosManager accesoriosManager = new AccesoriosManager(this);
        ArtefactoManager artefactoManager = new ArtefactoManager(this);
        GuardarropaManager wardrobeManager = new GuardarropaManager(this);
        MochilaManager mochilaManager = new MochilaManager(this);

        // El listener del guardarropa se instancia aquí porque el comando lo necesita
        GuardarropaListener wardrobeListener = new GuardarropaListener(wardrobeManager);

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

        // 🟢 REGISTRAMOS TODOS LOS LISTENERS QUE MUDAMOS DEL CORE
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerItemListener(this), this);
        getServer().getPluginManager().registerEvents(new VanillaStationsListener(this), this);

        // Nuevos Sistemas Modulares
        getServer().getPluginManager().registerEvents(new AccesoriosListener(this, accesoriosManager), this);
        getServer().getPluginManager().registerEvents(new ArtefactoListener(this, artefactoManager), this);
        getServer().getPluginManager().registerEvents(wardrobeListener, this);
        getServer().getPluginManager().registerEvents(new MochilaListener(mochilaManager), this);

        // ==========================================
        // 4. REGISTRO DE COMANDOS
        // ==========================================
        if (getCommand("desguace") != null) getCommand("desguace").setExecutor(new ComandoDesguace(this));
        if (getCommand("accesorios") != null) getCommand("accesorios").setExecutor(new ComandoAccesorios(accesoriosManager));
        if (getCommand("wardrobe") != null) getCommand("wardrobe").setExecutor(new ComandoWardrobe(wardrobeListener));
        if (getCommand("pv") != null) getCommand("pv").setExecutor(new ComandoPV(mochilaManager));

        // 🟢 REGISTRAMOS EL COMANDO DE PRUEBAS QUE MUDAMOS
        if (getCommand("test") != null) getCommand("test").setExecutor(new ComandoTest(this));

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        // 🟢 RESTAURAMOS LOS BLOQUES DE LAS HABILIDADES ANTES DE APAGAR (Failsafe)
        BlockBreakListener.restaurarBloquesRotos();
        getLogger().info("🗡️ NexoItems apagado y desconectado.");
    }

    // 🟢 Getter para que las demás clases puedan pedirle datos al FileManager
    public FileManager getFileManager() {
        return fileManager;
    }
}