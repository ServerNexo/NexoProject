package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
    @EventHandler(priority = EventPriority.HIGH)
    public void onColocarMinion(PlayerInteractEvent event) {
        // 🌟 CORRECCIÓN 1: Evitar doble ejecución por las dos manos (Mano principal y secundaria)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // Verificamos si es un Minion Oficial
        if (meta.getPersistentDataContainer().has(MinionKeys.TYPE, PersistentDataType.STRING)) {
            event.setCancelled(true); // Evitamos que ponga la cabeza/bloque físico
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

            try {
                String typeStr = meta.getPersistentDataContainer().get(MinionKeys.TYPE, PersistentDataType.STRING);

                // 🌟 CORRECCIÓN 2: Extracción segura del Nivel (Tier) para evitar crasheos silenciosos
                Integer tier = 1; // Valor por defecto
                if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.INTEGER)) {
                    tier = meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.INTEGER);
                } else if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.BYTE)) {
                    tier = (int) meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.BYTE);
                } else if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.STRING)) {
                    tier = Integer.parseInt(meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.STRING));
                }

                if (typeStr != null) {
                    int maxMinions = plugin.getMinionManager().getMaxMinions(player);
                    int placedMinions = plugin.getMinionManager().getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        player.sendMessage(NexoColor.parse("&#FF3366[!] Límite de Almas Alcanzado: &#E6CCFFMáximo de " + maxMinions + " esclavos permitidos."));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return; // 🛑 Detenemos el código aquí
                    }

                    MinionType type = MinionType.valueOf(typeStr);

                    // 🌟 CORRECCIÓN 3: Spawneo físico perfecto (Dependiendo de qué cara del bloque hizo clic)
                    Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

                    plugin.getMinionManager().spawnMinion(spawnLoc, player.getUniqueId(), type, tier);
                    plugin.getMinionManager().addPlacedMinion(player, 1);

                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>ESCLAVO CONJURADO:</bold> &#E6CCFFUnidad " + type.getDisplayName() + " atada al mundo terrenal. &#CC66FF(" + (placedMinions + 1) + "/" + maxMinions + ")"));
                    player.playSound(spawnLoc, org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.5f);
                }
            } catch (Exception e) {
                // 🌟 CORRECCIÓN 4: Capturamos cualquier error para que no haya fallos silenciosos
                player.sendMessage(NexoColor.parse("&#FF3366[!] Fallo de Invocación: &#E6CCFFEl sello de este esclavo está corrupto o es incompatible."));
                e.printStackTrace();
            }
        }
    }

    // 🟥 EVENTO 2: Romper el bloque debajo del Minion (La Gravedad)
    // 🌟 PARCHE DE SEGURIDAD APLICADO AQUÍ (Faltaba priority e ignoreCancelled)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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