package me.nexo.minions.listeners;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.manager.ActiveMinion;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MinionLoadListener implements Listener {
    private final NexoMinions plugin;

    public MinionLoadListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Escaneamos las entidades del chunk que acaba de cargar
        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof ItemDisplay display)) continue;

            // Revisamos si es un Minion (Si tiene dueño)
            var pdc = display.getPersistentDataContainer();
            if (!pdc.has(MinionKeys.OWNER, PersistentDataType.STRING)) continue;

            // Si ya está en la memoria (Manager), lo ignoramos
            if (plugin.getMinionManager().getMinion(display.getUniqueId()) != null) continue;

            // ¡Es un Minion huerfano! Lo leemos y lo reconectamos
            UUID ownerId = UUID.fromString(pdc.get(MinionKeys.OWNER, PersistentDataType.STRING));
            MinionType type = MinionType.valueOf(pdc.get(MinionKeys.TYPE, PersistentDataType.STRING));
            int tier = pdc.get(MinionKeys.TIER, PersistentDataType.INTEGER);
            long nextAction = pdc.get(MinionKeys.NEXT_ACTION, PersistentDataType.LONG);
            int stored = pdc.getOrDefault(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            // Buscamos su caja de colisiones (Hitbox) cercana
            Interaction hitbox = null;
            for (Entity nearby : display.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (nearby instanceof Interaction inter) {
                    String linkedId = inter.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
                    if (linkedId != null && linkedId.equals(display.getUniqueId().toString())) {
                        hitbox = inter;
                        break;
                    }
                }
            }

            // Lo metemos de vuelta a la memoria RAM de tu servidor
            plugin.getMinionManager().getMinionsActivos().put(display.getUniqueId(),
                    new ActiveMinion(plugin, display, hitbox, ownerId, type, tier, nextAction, stored));

            plugin.getLogger().info("🔌 ¡Minion reconectado en el Chunk " + event.getChunk().getX() + "," + event.getChunk().getZ() + "!");
        }
    }
}