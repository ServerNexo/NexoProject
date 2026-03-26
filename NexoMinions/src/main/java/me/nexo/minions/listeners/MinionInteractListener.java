package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT PARA LA PALETA GÓTICA DEL VACÍO
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.menu.MinionMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MinionInteractListener implements Listener {
    private final NexoMinions plugin;

    public MinionInteractListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    // 🖱️ CLIC DERECHO: Abrir Menú
    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction hitbox)) return;
        if (!esDueñoValido(event.getPlayer(), hitbox)) return;

        event.setCancelled(true);
        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);

        if (displayIdStr != null) {
            ActiveMinion minion = plugin.getMinionManager().getMinion(UUID.fromString(displayIdStr));
            if (minion != null) {
                // 🌟 EL CAMBIO MAESTRO: Le pasamos el 'plugin' y ahora también al 'Jugador' para la compatibilidad Bedrock
                event.getPlayer().openInventory(new MinionMenu(plugin, minion, event.getPlayer()).getInventory());
            }
        }
    }

    // 🗡️ CLIC IZQUIERDO: Golpear para Recoger al Minion
    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        event.setCancelled(true);
        if (!esDueñoValido(player, hitbox)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
        if (displayIdStr != null) {
            plugin.getMinionManager().recogerMinion(player, UUID.fromString(displayIdStr));
        }
    }

    private boolean esDueñoValido(Player player, Interaction hitbox) {
        NamespacedKey displayKey = new NamespacedKey(plugin, "minion_display_id");
        if (!hitbox.getPersistentDataContainer().has(displayKey, PersistentDataType.STRING)) return false;

        String ownerStr = hitbox.getPersistentDataContainer().get(MinionKeys.OWNER, PersistentDataType.STRING);
        if (ownerStr != null && !player.getUniqueId().toString().equals(ownerStr) && !player.hasPermission("nexominions.admin")) {
            // 🌟 Alerta Gótica de Infracción
            player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFEste esclavo obedece únicamente la voluntad de su Maestro."));
            return false;
        }
        return true;
    }
}