package me.nexo.mechanics;

import me.nexo.core.user.NexoAPI;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.commands.ComandoSkillsTabCompleter;
import me.nexo.mechanics.minigames.*;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMechanics extends JavaPlugin {

    private CombatComboManager combatComboManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ NexoMechanics: Iniciando Motores...");

        this.combatComboManager = new CombatComboManager(this);
        NexoAPI.getServices().register(CombatComboManager.class, this.combatComboManager);

        getServer().getPluginManager().registerEvents(this.combatComboManager, this);
        getServer().getPluginManager().registerEvents(new AlchemyMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new EnchantingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new FarmingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new FishingHookManager(this), this);
        getServer().getPluginManager().registerEvents(new MiningMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new WoodcuttingMinigameManager(this), this);

        if (getCommand("skilltree") != null) {
            getCommand("skilltree").setExecutor(new ComandoSkillTree());
            getCommand("skilltree").setTabCompleter(new ComandoSkillsTabCompleter());
        }

        getLogger().info("🔗 Conexión exitosa con NexoCore API");
        getLogger().info("⚙️ NexoMechanics activado correctamente.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        NexoAPI.getServices().unregister(CombatComboManager.class);
        getLogger().info("⚙️ NexoMechanics: Motores apagados.");
    }

    public CombatComboManager getCombatComboManager() {
        return combatComboManager;
    }
}