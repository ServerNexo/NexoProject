package me.nexo.protections.listeners;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EnvironmentListener implements Listener {

    private final ClaimManager claimManager;

    public EnvironmentListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    // 🛡️ BLOQUEAR APERTURA DE COFRES, HORNOS, PUERTAS
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;

        // No bloqueamos los ataques a monstruos con clic izquierdo
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        if (stone == null) return; // Si no hay protección, todo está permitido

        // Si es el dueño, tiene permiso absoluto
        if (stone.getOwnerId().equals(player.getUniqueId())) return;

        // Si es un acólito de confianza o del clan, tiene permiso
        if (stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) return;

        Material tipo = block.getType();
        String typeStr = tipo.toString();

        // 1. REVISAR LEY DE CONTENEDORES (Cofres, Hornos, Barriles, Shulkers)
        if (typeStr.contains("CHEST") || typeStr.contains("FURNACE") || typeStr.contains("BARREL")
                || typeStr.contains("SHULKER") || typeStr.contains("HOPPER") || typeStr.contains("DROPPER")) {

            // Si la ley "containers" está en false, lo bloqueamos
            if (!stone.getFlag("containers")) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, "&#FF3366[!] Herejía: &#E6CCFFLos tesoros de este dominio están sellados.");
                return;
            }
        }

        // 2. REVISAR LEY DE INTERACCIÓN (Puertas, Botones, Palancas, Yunques)
        if (typeStr.contains("DOOR") || typeStr.contains("BUTTON") || typeStr.contains("LEVER")
                || typeStr.contains("TRAPDOOR") || typeStr.contains("ANVIL") || typeStr.contains("CRAFTING")) {

            // Si la ley "interact" está en false, lo bloqueamos
            if (!stone.getFlag("interact")) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, "&#FF3366[!] Herejía: &#E6CCFFNo tienes permiso para interactuar aquí.");
            }
        }
    }

    // 🛡️ BLOQUEAR TIRAR ÍTEMS AL SUELO
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ProtectionStone stone = claimManager.getStoneAt(player.getLocation());

        if (stone == null) return;
        if (stone.getOwnerId().equals(player.getUniqueId())) return;
        if (stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) return;

        // Si la ley "item-drop" está en false, bloqueamos que ensucie el suelo
        if (!stone.getFlag("item-drop")) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF3366[!] Dominio Puro: &#E6CCFFNo puedes arrojar ofrendas al suelo en tierras ajenas.");
        }
    }
}