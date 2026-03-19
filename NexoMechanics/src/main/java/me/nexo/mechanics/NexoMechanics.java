package me.nexo.mechanics;

import me.nexo.mechanics.minigames.*;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMechanics extends JavaPlugin {

    // Variables para acceder a los managers si otros plugins lo necesitan
    private CombatComboManager combatComboManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ NexoMechanics: Iniciando Motores...");

        // 1. Inicializar Managers que requieren persistencia o variables accesibles
        this.combatComboManager = new CombatComboManager(this);

        // 2. Registrar todos los Eventos de Minijuegos
        getServer().getPluginManager().registerEvents(this.combatComboManager, this);
        getServer().getPluginManager().registerEvents(new AlchemyMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new EnchantingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new FarmingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new FishingHookManager(this), this);
        getServer().getPluginManager().registerEvents(new MiningMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new WoodcuttingMinigameManager(this), this);

        getLogger().info("🔗 Conexión exitosa con NexoCore API");
        getLogger().info("⚙️ NexoMechanics activado correctamente.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚙️ NexoMechanics: Motores apagados.");
    }

    /**
     * Getter para obtener el CombatComboManager desde otros Addons
     */
    public CombatComboManager getCombatComboManager() {
        return combatComboManager;
    }
}