package me.nexo.minions.listeners;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

/**
 * 🤖 NexoMinions - Listener Principal de Bloques (Arquitectura Enterprise)
 */
public class MinionListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;
    private final UpgradesConfig upgradesConfig;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public MinionListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager, UpgradesConfig upgradesConfig) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;
    }

    // 🟩 EVENTO 1: Colocar el Minion
    @EventHandler(priority = EventPriority.HIGH)
    public void onColocarMinion(PlayerInteractEvent event) {
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

            // 🌟 Integración Desacoplada con NexoProtections
            Optional<ClaimManager> claimManagerOpt = NexoAPI.getServices().get(ClaimManager.class);
            if (claimManagerOpt.isPresent()) {
                ProtectionStone stone = claimManagerOpt.get().getStoneAt(event.getClickedBlock().getLocation());
                if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().manager().dominioAjeno());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return; // 🛑 Cortamos el flujo
                }
            }

            try {
                String typeStr = meta.getPersistentDataContainer().get(MinionKeys.TYPE, PersistentDataType.STRING);

                Integer tier = 1; // Valor por defecto
                if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.INTEGER)) {
                    tier = meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.INTEGER);
                } else if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.BYTE)) {
                    tier = (int) meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.BYTE);
                } else if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.STRING)) {
                    tier = Integer.parseInt(meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.STRING));
                }

                if (typeStr != null) {
                    int maxMinions = minionManager.getMaxMinions(player);
                    int placedMinions = minionManager.getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        CrossplayUtils.sendMessage(player, configManager.getMessages().manager().limiteAlcanzado().replace("%max%", String.valueOf(maxMinions)));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return; // 🛑 Detenemos el código aquí
                    }

                    MinionType type = MinionType.valueOf(typeStr);
                    Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

                    minionManager.spawnMinion(spawnLoc, player.getUniqueId(), type, tier);
                    minionManager.addPlacedMinion(player, 1);

                    item.setAmount(item.getAmount() - 1);
                    String msg = configManager.getMessages().manager().esclavoConjurado()
                            .replace("%type%", type.getDisplayName())
                            .replace("%placed%", String.valueOf(placedMinions + 1))
                            .replace("%max%", String.valueOf(maxMinions));

                    CrossplayUtils.sendMessage(player, msg);
                    player.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.5f);
                }
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().selloCorrupto());
                e.printStackTrace();
            }
        }
    }

    // 🟥 EVENTO 2: Romper el bloque debajo del Minion (La Gravedad)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location topLoc = event.getBlock().getLocation().add(0.5, 1.0, 0.5);
        Player player = event.getPlayer();

        for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof Interaction hitbox) {
                String displayIdStr = hitbox.getPersistentDataContainer().get(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING);

                if (displayIdStr != null) {
                    ActiveMinion minion = minionManager.getMinion(UUID.fromString(displayIdStr));

                    if (minion != null) {
                        // 🌟 SEGURIDAD ABSOLUTA
                        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
                            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().desestabilizarAjeno());
                            event.setCancelled(true);
                            return;
                        }

                        minionManager.recogerMinion(player, UUID.fromString(displayIdStr));
                        break;
                    }
                }
            }
        }
    }

    // =========================================
    // 🛡️ PROTECCIÓN DE ÍTEMS ARCANOS (MEJORAS)
    // =========================================

    @EventHandler
    public void onColocarMejora(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType().isAir()) return;

        if (upgradesConfig.getUpgradeData(item) != null) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoBloque());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onDerramarLava(PlayerBucketEmptyEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != event.getBucket()) {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }

        if (item.getType() != org.bukkit.Material.AIR) {
            if (upgradesConfig.getUpgradeData(item) != null) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoLiquido());
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onInteractuarConMejora(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType().isAir()) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (upgradesConfig.getUpgradeData(event.getItem()) != null) {
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            }
        }
    }
}