package me.nexo.colecciones.colecciones;

// 🟢 ARQUITECTURA: Importamos tu nuevo Addon
import me.nexo.colecciones.NexoColecciones;

// 🟢 MANTENEMOS ESTO: Importamos el Core para pedirle la conexión a Supabase
import me.nexo.core.NexoCore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ColeccionesListener implements Listener {

    // 🟢 ARQUITECTURA: Usamos NexoColecciones como dueño del evento
    private final NexoColecciones plugin;
    private final ColeccionesConfig config;

    // 🟢 INYECCIÓN DE DEPENDENCIAS: Pedimos el plugin y la config
    public ColeccionesListener(NexoColecciones plugin, ColeccionesConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 🟢 COMUNICACIÓN ENTRE MICROSERVICIOS: Le pedimos el DataSource al Core
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        CollectionManager.loadPlayerFromDatabase(event.getPlayer().getUniqueId(), core.getDatabaseManager().getDataSource());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Se borra de la RAM (El guardado lo hace FlushTask)
        CollectionManager.removeProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        // Marcamos el bloque como "puesto por un jugador" usando nuestro Addon
        event.getBlock().setMetadata("player_placed", new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("player_placed")) return;

        String blockId = event.getBlock().getType().name().toLowerCase();

        // 🟢 Usamos la nueva variable config
        if (config.esColeccion(blockId)) {
            CollectionProfile profile = CollectionManager.getProfile(event.getPlayer().getUniqueId());
            if (profile != null) profile.addProgress(blockId, 1, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobId = event.getEntity().getType().name().toLowerCase();

        // 🟢 Usamos la nueva variable config
        if (config.esSlayer(mobId)) {
            CollectionProfile profile = CollectionManager.getProfile(killer.getUniqueId());
            if (profile != null) {
                int xpPorKill = config.getDatosSlayer(mobId).getInt("xp_por_kill", 1);
                profile.addProgress("slayer_" + mobId, xpPorKill, true);
            }
        }
    }
}