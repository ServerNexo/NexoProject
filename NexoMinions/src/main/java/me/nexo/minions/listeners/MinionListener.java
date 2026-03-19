package me.nexo.minions.listeners;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MinionListener implements Listener {
    private final NexoMinions plugin;

    public MinionListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onColocarMinion(PlayerInteractEvent event) {
        // Solo nos importa si dan clic derecho a un bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey keyType = new NamespacedKey(plugin, "minion_type");
        NamespacedKey keyTier = new NamespacedKey(plugin, "minion_tier");

        // Verificamos si el ítem en la mano tiene el "ADN" de un Minion
        if (meta.getPersistentDataContainer().has(keyType, PersistentDataType.STRING)) {
            event.setCancelled(true); // Evitamos que pongan la cabeza de jugador normal

            String typeStr = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
            Integer tier = meta.getPersistentDataContainer().get(keyTier, PersistentDataType.INTEGER);

            if (typeStr != null && tier != null) {
                try {
                    MinionType type = MinionType.valueOf(typeStr);
                    // Calculamos el centro del bloque de arriba
                    Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);

                    // 🌟 AQUÍ LLAMAMOS A LA MAGIA DEL MANAGER
                    plugin.getMinionManager().spawnMinion(spawnLoc, event.getPlayer().getUniqueId(), type, tier);

                    // Le quitamos el ítem de la mano
                    item.setAmount(item.getAmount() - 1);

                    event.getPlayer().sendMessage(ChatColor.GREEN + "🐝 ¡" + type.getDisplayName() + " ha comenzado a trabajar!");
                } catch (IllegalArgumentException e) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Error: Este minion está corrupto.");
                }
            }
        }
    }
}