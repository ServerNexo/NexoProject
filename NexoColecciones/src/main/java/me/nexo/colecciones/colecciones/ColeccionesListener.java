package me.nexo.colecciones.colecciones;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.nexo.core.NexoCore;

public class ColeccionesListener implements Listener {

    private final NexoColecciones plugin;
    private final CollectionManager manager;
    private static final String ANTI_EXPLOIT_KEY = "nexo_player_placed";

    public ColeccionesListener(NexoColecciones plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCollectionManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Marcamos el bloque como "Puesto por un jugador"
        event.getBlock().setMetadata(ANTI_EXPLOIT_KEY, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Si tiene la marca, abortamos y no sumamos puntos
        if (block.hasMetadata(ANTI_EXPLOIT_KEY)) {
            block.removeMetadata(ANTI_EXPLOIT_KEY, plugin);
            return;
        }

        // Si es un bloque natural, sumamos 1 punto a su colección
        String blockId = block.getType().name();
        manager.addProgress(event.getPlayer(), blockId, 1);
    }

    // Asegúrate de importar esto arriba:
    // import org.bukkit.event.player.PlayerJoinEvent;
    // import org.bukkit.event.player.PlayerQuitEvent;
    // import me.nexo.core.NexoCore;

    // 📥 1. CUANDO EL JUGADOR ENTRA: Cargamos sus colecciones desde Supabase
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Le pedimos la conexión de base de datos prestada al Core
        var dataSource = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getDataSource();

        // Le decimos al Manager que cargue a este jugador en la RAM
        manager.loadPlayerFromDatabase(event.getPlayer().getUniqueId(), dataSource);
    }

    // 📤 2. CUANDO EL JUGADOR SALE: Limpiamos la RAM
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Borramos su perfil de la memoria para no causar lag
        manager.removeProfile(event.getPlayer().getUniqueId());
    }

}