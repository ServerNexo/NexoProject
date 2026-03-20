package me.nexo.colecciones.colecciones;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

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
}