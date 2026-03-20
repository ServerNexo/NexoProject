package me.nexo.minions.listeners;

import me.nexo.minions.NexoMinions;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

public class ExplosionListener implements Listener {
    private final NexoMinions plugin;

    public ExplosionListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    // 💥 Cubre explosiones de entidades (Creepers, TNT, Wither, Fireballs)
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        protegerSueloMinions(event.blockList());
    }

    // 💥 Cubre explosiones de bloques (Camas en el Nether, Respawn Anchors)
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        protegerSueloMinions(event.blockList());
    }

    private void protegerSueloMinions(java.util.List<Block> bloquesDestruidos) {
        // Usamos un Iterator porque vamos a eliminar elementos de la lista en tiempo real
        Iterator<Block> iterator = bloquesDestruidos.iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            // Calculamos el espacio justo encima del bloque que va a explotar
            Location topLoc = block.getLocation().add(0.5, 1.0, 0.5);

            // Escaneamos si hay un Minion encima de este bloque
            for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
                if (entity instanceof Interaction hitbox) {
                    String displayIdStr = hitbox.getPersistentDataContainer().get(
                            new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);

                    if (displayIdStr != null) {
                        // ¡Hay un Minion aquí! Removemos este bloque específico de la explosión
                        iterator.remove();
                        break; // Pasamos al siguiente bloque
                    }
                }
            }
        }
    }
}