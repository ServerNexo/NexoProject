package me.nexo.protections;

import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoProtections extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Iniciando NexoProtections (Motor Híbrido)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("NexoClans") == null) {
            getLogger().warning("⚠️ NexoClans no detectado. Las protecciones de clan estarán desactivadas.");
        }

        // 🚧 Aquí inicializaremos los Managers en el siguiente paso

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🛡️ NexoProtections apagado. Guardando datos asíncronos...");
    }
}