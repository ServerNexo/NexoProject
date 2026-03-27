package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor;
import me.nexo.minions.NexoMinions;
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

        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
        if (displayIdStr == null) return;

        ActiveMinion minion = plugin.getMinionManager().getMinion(UUID.fromString(displayIdStr));
        if (minion == null) return;

        Player player = event.getPlayer();

        // 🌟 PARCHE DE SEGURIDAD ABSOLUTA: Consultar a la memoria RAM, no a la Hitbox
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFEste esclavo obedece únicamente la voluntad de su Maestro."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        player.openInventory(new MinionMenu(plugin, minion, player).getInventory());
    }

    // 🗡️ CLIC IZQUIERDO: Golpear para Recoger al Minion
    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
        if (displayIdStr == null) return;

        ActiveMinion minion = plugin.getMinionManager().getMinion(UUID.fromString(displayIdStr));
        if (minion == null) return;

        // 🌟 PARCHE DE SEGURIDAD ABSOLUTA
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFNo puedes desterrar al esclavo de otro Maestro."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        plugin.getMinionManager().recogerMinion(player, UUID.fromString(displayIdStr));
    }
}