package me.nexo.minions.listeners;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
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

/**
 * 🤖 NexoMinions - Listener de Interacción (Arquitectura Enterprise)
 */
public class MinionInteractListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    @Inject
    public MinionInteractListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
    }

    // 🖱️ CLIC DERECHO: Abrir Menú
    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction hitbox)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
        if (displayIdStr == null) return;

        ActiveMinion minion = minionManager.getMinion(UUID.fromString(displayIdStr));
        if (minion == null) return;

        Player player = event.getPlayer();

        // 🌟 PARCHE DE SEGURIDAD ABSOLUTA
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().interactuarAjeno());
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        new MinionMenu(player, plugin, minion).open();
    }

    // 🗡️ CLIC IZQUIERDO: Golpear para Recoger al Minion
    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);
        if (displayIdStr == null) return;

        ActiveMinion minion = minionManager.getMinion(UUID.fromString(displayIdStr));
        if (minion == null) return;

        // 🌟 PARCHE DE SEGURIDAD ABSOLUTA
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().desterrarAjeno());
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        minionManager.recogerMinion(player, UUID.fromString(displayIdStr));
    }
}