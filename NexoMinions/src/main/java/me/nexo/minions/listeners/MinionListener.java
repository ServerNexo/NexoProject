package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT PARA LA PALETA GÓTICA DEL VACÍO
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.manager.ActiveMinion;
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

        // 🌟 USAR LAS LLAVES OFICIALES
        if (meta.getPersistentDataContainer().has(MinionKeys.TYPE, PersistentDataType.STRING)) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            // 🌟 INTEGRACIÓN: Verificar derechos de propiedad territorial ANTES de hacer nada
            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                ProtectionStone stone = NexoProtections.getClaimManager().getStoneAt(event.getClickedBlock().getLocation());
                if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
                    player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFEste dominio pertenece a otra entidad."));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return; // 🛑 Cortamos el flujo
                }
            }

            String typeStr = meta.getPersistentDataContainer().get(MinionKeys.TYPE, PersistentDataType.STRING);
            Integer tier = meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.INTEGER);

            if (typeStr != null && tier != null) {
                try {
                    // Verificamos los límites de minions del jugador
                    int maxMinions = plugin.getMinionManager().getMaxMinions(player);
                    int placedMinions = plugin.getMinionManager().getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        player.sendMessage(NexoColor.parse("&#FF3366[!] Límite de Almas Alcanzado: &#E6CCFFMáximo de " + maxMinions + " esclavos permitidos."));
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
                    player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>ESCLAVO CONJURADO:</bold> &#E6CCFFUnidad " + type.getDisplayName() + " atada al mundo terrenal. &#CC66FF(" + (placedMinions + 1) + "/" + maxMinions + ")"));
                } catch (IllegalArgumentException e) {
                    event.getPlayer().sendMessage(NexoColor.parse("&#FF3366[!] Fallo de Invocación: &#E6CCFFEl sello de este esclavo está corrupto."));
                }
            }
        }
    }

    // 🟥 EVENTO 2: Romper el bloque debajo del Minion (La Gravedad)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location topLoc = event.getBlock().getLocation().add(0.5, 1.0, 0.5);

        for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof Interaction hitbox) {
                String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);

                if (displayIdStr != null) {
                    ActiveMinion minion = plugin.getMinionManager().getMinion(UUID.fromString(displayIdStr));

                    if (minion != null) {
                        // 🌟 SEGURIDAD ABSOLUTA
                        if (!minion.getOwnerId().equals(event.getPlayer().getUniqueId()) && !event.getPlayer().hasPermission("nexominions.admin")) {
                            event.getPlayer().sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFNo puedes desestabilizar el ritual de un esclavo ajeno."));
                            event.setCancelled(true);
                            return;
                        }

                        plugin.getMinionManager().recogerMinion(event.getPlayer(), UUID.fromString(displayIdStr));
                        break;
                    }
                }
            }
        }
    }

    // =========================================
    // 🛡️ PROTECCIÓN DE ÍTEMS ARCANOS (MEJORAS)
    // =========================================

    // 🚫 1. Evitar que coloquen las mejoras como bloques (Ej: Pistones, Cofres)
    @EventHandler
    public void onColocarMejora(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType().isAir()) return;

        // Si el ítem está registrado en upgrades.yml, bloqueamos su colocación
        if (plugin.getUpgradesConfig().getUpgradeData(item) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFEste sello arcano solo puede ser depositado dentro de un Esclavo."));
            event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // 🚫 2. Evitar que derramen líquidos (Ej: Cubos de Lava custom)
    @EventHandler
    public void onDerramarLava(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != event.getBucket()) {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }

        if (item.getType() != org.bukkit.Material.AIR) {
            if (plugin.getUpgradesConfig().getUpgradeData(item) != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFEsta materia inestable pertenece a las entidades del vacío."));
                event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    // 🚫 3. Evitar interacciones accidentales (Consumir, usar en bloques, etc)
    @EventHandler
    public void onInteractuarConMejora(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType().isAir()) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (plugin.getUpgradesConfig().getUpgradeData(event.getItem()) != null) {
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            }
        }
    }
}