package me.nexo.protections.listeners;

import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class EnvironmentListener implements Listener {

    private final ClaimManager claimManager;

    public EnvironmentListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    // 💥 BLOQUEAR EXPLOSIONES (Creepers, TNT, Wither, etc.)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // En lugar de cancelar toda la explosión (lo que dejaría a la TNT sin efecto fuera de la base),
        // filtramos la lista de bloques y "salvamos" a los que están protegidos.
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null && !stone.getFlag("tnt-damage")) {
                it.remove(); // Sacamos el bloque de la explosión
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null && !stone.getFlag("tnt-damage")) {
                it.remove();
            }
        }
    }

    // 🔥 BLOQUEAR FUEGO Y LAVA
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        ProtectionStone stone = claimManager.getStoneAt(event.getBlock().getLocation());
        if (stone != null && !stone.getFlag("fire-spread")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        ProtectionStone stone = claimManager.getStoneAt(event.getBlock().getLocation());
        if (stone != null && !stone.getFlag("fire-spread")) {
            event.setCancelled(true);
        }
    }

    // 🧟 BLOQUEAR APARICIÓN DE MONSTRUOS (Zombis, Esqueletos, etc.)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        // Solo bloqueamos Monstruos hostiles. ¡Los cerdos y vacas sí pueden nacer en la base!
        if (event.getEntity() instanceof Monster) {
            ProtectionStone stone = claimManager.getStoneAt(event.getLocation());
            if (stone != null && !stone.getFlag("mob-spawning")) {
                // Excepción: Si el monstruo nació de un Spawner, lo dejamos (para granjas de experiencia)
                if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
                    event.setCancelled(true);
                }
            }
        }
    }
}