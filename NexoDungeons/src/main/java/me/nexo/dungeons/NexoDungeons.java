package me.nexo.dungeons;

import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.grid.DungeonGridManager;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager; // 🌟 IMPORT AÑADIDO
import me.nexo.dungeons.waves.WaveManager;
import me.nexo.dungeons.bosses.BossFightManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoDungeons extends JavaPlugin {

    private DungeonGridManager gridManager;
    private PuzzleEngine puzzleEngine;
    private WaveManager waveManager;
    private QueueManager queueManager; // 🌟 VARIABLE AÑADIDA

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏰 Iniciando NexoDungeons (Hybrid Engine)...");

        // Verificamos dependencias críticas
        if (getServer().getPluginManager().getPlugin("NexoCore") == null ||
                getServer().getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().severe("❌ Error: Faltan NexoCore o MythicMobs. Apagando NexoDungeons...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 1. Inicializar Grid Manager (Mundo vacío y FAWE)
        this.gridManager = new DungeonGridManager(this);

        // 2. Inicializar Puzzle Engine (El Cerebro JSON)
        this.puzzleEngine = new PuzzleEngine(this);

        // 3. Inicializar Wave Manager (El Director de Combate)
        this.waveManager = new WaveManager(this);

        // 🌟 NUEVO: Inicializar Motor de Colas (Matchmaking)
        this.queueManager = new QueueManager(this);

        // 4. Registrar Listeners Base
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(this.waveManager, this);
        getServer().getPluginManager().registerEvents(new BossFightManager(this), this);
        getServer().getPluginManager().registerEvents(new LootProtectionListener(this), this);

        // 5. Registramos la Seguridad Anti-Dupe
        getServer().getPluginManager().registerEvents(new me.nexo.dungeons.listeners.DungeonSecurityListener(this), this);

        // 6. Registramos los Menús y el Comando principal
        getServer().getPluginManager().registerEvents(new me.nexo.dungeons.menu.DungeonMenuListener(this), this);

        // Verificamos que el comando esté en el plugin.yml para evitar errores de NullPointer
        if (getCommand("dungeons") != null) {
            getCommand("dungeons").setExecutor(new me.nexo.dungeons.commands.ComandoDungeon());
        } else {
            getLogger().warning("⚠️ El comando 'dungeons' no fue encontrado en el plugin.yml.");
        }

        getLogger().info("✅ ¡Dungeons, Puzzles, Oleadas, Loot Seguro, Menús y Colas en línea!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Apagando NexoDungeons... Limpiando memoria.");
    }

    // =========================================
    // 🔌 GETTERS
    // =========================================

    public DungeonGridManager getGridManager() {
        return gridManager;
    }

    public PuzzleEngine getPuzzleEngine() {
        return puzzleEngine;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    // 🌟 NUEVO: GETTER AÑADIDO
    public QueueManager getQueueManager() {
        return queueManager;
    }
}