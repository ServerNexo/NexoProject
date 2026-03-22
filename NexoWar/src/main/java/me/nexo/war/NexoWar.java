package me.nexo.war;

import me.nexo.war.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NexoWar: Sistema Nativo de Guerras de Honor y Apuestas.
 * Se integra con NexoCore (DB), NexoClans (Social), NexoEconomy (Money) y NexoProtections (World).
 */
public class NexoWar extends JavaPlugin {

    // 🌟 Instancia única del Manager para gestionar las apuestas y contratos
    private WarManager warManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ Iniciando NexoWar (Sistema de Guerras y Apuestas)...");

        // 1. Verificación estricta de nuestras propias APIs nativas
        // Comprobamos que el ecosistema Nexo esté presente y cargado
        if (getServer().getPluginManager().getPlugin("NexoCore") == null ||
                getServer().getPluginManager().getPlugin("NexoClans") == null ||
                getServer().getPluginManager().getPlugin("NexoEconomy") == null ||
                getServer().getPluginManager().getPlugin("NexoProtections") == null) {

            getLogger().severe("❌ Error Crítico: Faltan dependencias nativas del ecosistema Nexo.");
            getLogger().severe("Asegúrate de tener instalados: Core, Clans, Economy y Protections.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Inicialización del Motor de Guerra (Escrow y Contratos)
        // El WarManager se encarga de la persistencia en Supabase y la lógica de apuestas
        this.warManager = new WarManager(this);

        // 🚧 Siguientes pasos:
        // - Registro de ComandoWar para gestionar los desafíos (/war challenge)
        // - Registro de WarListener para el control de PvP y muertes (KillTracker)

        getLogger().info("✅ ¡NexoWar cargado exitosamente! Los clanes están listos para la batalla.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("⚔️ NexoWar apagado. El estado de las guerras activas está a salvo en la base de datos.");
    }

    /**
     * Obtiene el gestor de guerras del plugin.
     * @return Instancia activa de WarManager.
     */
    public WarManager getWarManager() {
        return warManager;
    }
}