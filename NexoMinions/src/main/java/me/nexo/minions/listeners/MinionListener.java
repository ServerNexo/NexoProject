package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.core.ClaimAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MinionListener implements Listener {
    private final NexoMinions plugin;

    public MinionListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    // 🟩 EVENTO 1: Colocar el Minion
    @EventHandler
    public void onColocarMinion(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey keyType = new NamespacedKey(plugin, "minion_type");
        NamespacedKey keyTier = new NamespacedKey(plugin, "minion_tier");

        if (meta.getPersistentDataContainer().has(keyType, PersistentDataType.STRING)) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            // 🌟 INTEGRACIÓN: Verificar derechos de propiedad territorial ANTES de hacer nada
            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                ProtectionStone stone = NexoProtections.getClaimManager().getStoneAt(event.getClickedBlock().getLocation());
                if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
                    player.sendMessage(NexoColor.parse("&#FF5555[!] Acceso Denegado: Territorio corporativo ajeno."));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return; // 🛑 Cortamos el flujo
                }
            }

            String typeStr = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
            Integer tier = meta.getPersistentDataContainer().get(keyTier, PersistentDataType.INTEGER);

            if (typeStr != null && tier != null) {
                try {
                    // Verificamos los límites de minions del jugador
                    int maxMinions = plugin.getMinionManager().getMaxMinions(player);
                    int placedMinions = plugin.getMinionManager().getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        player.sendMessage(NexoColor.parse("&#FF5555[!] Límite Operativo Alcanzado: &#AAAAAAMáximo de " + maxMinions + " operarios permitidos."));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return; // 🛑 Detenemos el código aquí
                    }

                    // Si tiene espacio y permiso de tierra, lo spawneamos
                    MinionType type = MinionType.valueOf(typeStr);
                    Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);

                    plugin.getMinionManager().spawnMinion(spawnLoc, player.getUniqueId(), type, tier);

                    // Registramos que el jugador gastó 1 espacio
                    plugin.getMinionManager().addPlacedMinion(player, 1);

                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>OPERARIO DESPLEGADO:</bold> &#AAAAAAUnidad " + type.getDisplayName() + " en línea. &#00E5FF(" + (placedMinions + 1) + "/" + maxMinions + ")"));
                } catch (IllegalArgumentException e) {
                    event.getPlayer().sendMessage(NexoColor.parse("&#FF5555[!] Error de Firmware: El disco de datos de este operario está corrupto."));
                }
            }
        }
    }

    // 🟥 EVENTO 2: Romper el bloque debajo del Minion (La Gravedad)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Calculamos la posición exacta justo arriba del bloque que se está rompiendo
        Location topLoc = event.getBlock().getLocation().add(0.5, 1.0, 0.5);

        // Escaneamos un radio muy pequeño (0.5 bloques) buscando entidades
        for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {

            // Si encontramos una Hitbox (Interaction)...
            if (entity instanceof Interaction hitbox) {
                String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);

                // ... y está vinculada a un Minion
                if (displayIdStr != null) {
                    String ownerStr = hitbox.getPersistentDataContainer().get(MinionKeys.OWNER, PersistentDataType.STRING);

                    // 1. Verificamos si el que rompe el bloque NO es el dueño
                    if (ownerStr != null && !event.getPlayer().getUniqueId().toString().equals(ownerStr) && !event.getPlayer().hasPermission("nexominions.admin")) {
                        event.getPlayer().sendMessage(NexoColor.parse("&#FF5555[!] Infracción de Seguridad: No puedes desestabilizar la plataforma de un operario ajeno."));
                        event.setCancelled(true);
                        return;
                    }

                    // 2. Si es el dueño (o un admin), forzamos la recolección del minion
                    plugin.getMinionManager().recogerMinion(event.getPlayer(), UUID.fromString(displayIdStr));

                    // Nota: No cancelamos el evento de romper el bloque,
                    // permitimos que el bloque se rompa, pero el minion se va al inventario en lugar de flotar.
                    break;
                }
            }
        }
    }
}