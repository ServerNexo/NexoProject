package me.nexo.protections.listeners;

import com.google.inject.Inject;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.LimitManager;
import me.nexo.protections.menu.ProtectionMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 🛡️ NexoProtections - Listener Principal (Arquitectura Enterprise)
 * Cero llamadas estáticas.
 */
public class ProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final LimitManager limitManager;
    private final ConfigManager configManager;
    private final NexoCore core;
    private final NexoProtections plugin;
    private final UserManager userManager;

    private final NamespacedKey isProtectionStoneKey;

    // 💉 PILAR 3: Inyección masiva
    @Inject
    public ProtectionListener(ClaimManager claimManager, LimitManager limitManager, ConfigManager configManager, NexoCore core, NexoProtections plugin) {
        this.claimManager = claimManager;
        this.limitManager = limitManager;
        this.configManager = configManager;
        this.core = core;
        this.plugin = plugin;
        this.userManager = core.getUserManager();
        this.isProtectionStoneKey = new NamespacedKey(plugin, "is_protection_stone");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        if (stone != null && block.getType() == Material.LODESTONE) {
            if (stone.getOwnerId().equals(player.getUniqueId())) {
                stone.removeHologram();
                claimManager.removeStoneFromCache(stone);

                CompletableFuture.runAsync(() -> {
                    try (Connection conn = core.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });

                event.setDropItems(false);

                ItemStack stoneItem = new ItemStack(Material.LODESTONE);
                ItemMeta meta = stoneItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(
                            configManager.getMessages().mensajes().items().selloAbismoNombre()
                    ));
                    meta.lore(configManager.getMessages().mensajes().items().selloAbismoLore().stream()
                            .map(line -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                            .toList());
                    meta.getPersistentDataContainer().set(isProtectionStoneKey, PersistentDataType.BYTE, (byte) 1);
                    stoneItem.setItemMeta(meta);
                }
                block.getWorld().dropItemNaturally(block.getLocation(), stoneItem);

                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().ritualDeshecho());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
                return;
            } else {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().noDestruirAjeno());
                return;
            }
        }

        if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().dominioSellado());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());
        if (existingStone != null && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinConstruirAjeno());
            return;
        }

        ItemStack itemInHand = event.getItemInHand();

        if (block.getType() == Material.LODESTONE && itemInHand.hasItemMeta()) {
            if (!itemInHand.getItemMeta().getPersistentDataContainer().has(isProtectionStoneKey, PersistentDataType.BYTE)) {
                return;
            }

            Location loc = block.getLocation();
            int radius = limitManager.getProtectionRadius(player);
            UUID newStoneId = UUID.randomUUID();
            NexoUser user = userManager.getUserOrNull(player.getUniqueId());
            UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

            ClaimBox newBox = new ClaimBox(loc.getWorld().getName(), loc.getBlockX()-radius, -64, loc.getBlockZ()-radius, loc.getBlockX()+radius, 320, loc.getBlockZ()+radius);

            if (claimManager.hasOverlappingClaim(newBox)) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().colisionEnergia());
                return;
            }

            ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);
            claimManager.addStoneToCache(newStone);
            newStone.updateHologram();

            CrossplayUtils.sendActionBar(player, "&#ff00ff[⟳] Conectando con el Abismo...");

            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                if (!canPlace) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        block.setType(Material.AIR);
                        newStone.removeHologram();
                        claimManager.removeStoneFromCache(newStone);
                        ItemStack refundItem = itemInHand.clone();
                        refundItem.setAmount(1);
                        player.getInventory().addItem(refundItem);
                        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().limiteAlcanzado());
                    });
                    return;
                }

                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().selloInvocado().replace("%radio%", String.valueOf(radius)));
                player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 0.5f);

                CompletableFuture.runAsync(() -> {
                    String sql = "INSERT INTO nexo_protections (stone_id, owner_id, clan_id, world_name, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (CAST(? AS UUID), CAST(? AS UUID), " + (clanId == null ? "NULL" : "CAST(? AS UUID)") + ", ?, ?, ?, ?, ?, ?, ?)";
                    try (Connection conn = core.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, newStoneId.toString());
                        ps.setString(2, player.getUniqueId().toString());
                        int index = 3;
                        if (clanId != null) ps.setString(index++, clanId.toString());
                        ps.setString(index++, newBox.world());
                        ps.setInt(index++, newBox.minX());
                        ps.setInt(index++, newBox.minY());
                        ps.setInt(index++, newBox.minZ());
                        ps.setInt(index++, newBox.maxX());
                        ps.setInt(index++, newBox.maxY());
                        ps.setInt(index++, newBox.maxZ());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.LODESTONE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null) {
                event.setCancelled(true);

                if (stone.getOwnerId().equals(player.getUniqueId()) || stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) {
                    new ProtectionMenu(player, plugin, stone).open();
                } else {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().monolitoRechaza());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        ProtectionStone fromClaim = claimManager.getStoneAt(event.getFrom());
        ProtectionStone toClaim = claimManager.getStoneAt(event.getTo());

        if (fromClaim != toClaim) {

            if (toClaim != null) {

                if (!toClaim.getFlag("ENTRY") && !toClaim.getOwnerId().equals(player.getUniqueId()) && !toClaim.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) && !player.hasPermission("nexoprotections.admin")) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().campoFuerza());

                    Vector pushback = event.getFrom().toVector().subtract(event.getTo().toVector()).normalize().multiply(0.5);
                    pushback.setY(0.1);
                    player.setVelocity(pushback);

                    event.setCancelled(true);
                    return;
                }

                String ownerName = Bukkit.getOfflinePlayer(toClaim.getOwnerId()).getName();
                if (ownerName == null) ownerName = "Desconocido";
                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaProtegida().replace("%owner%", ownerName));
            }

            if (fromClaim != null && toClaim == null) {
                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaSalvaje());
            }
        }
    }
}