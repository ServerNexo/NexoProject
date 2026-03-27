package me.nexo.protections.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.LimitManager;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final LimitManager limitManager;
    private final NexoCore core;
    private final NamespacedKey isProtectionStoneKey;

    public ProtectionListener(ClaimManager claimManager, LimitManager limitManager) {
        this.claimManager = claimManager;
        this.limitManager = limitManager;
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.isProtectionStoneKey = new NamespacedKey(NexoProtections.getPlugin(NexoProtections.class), "is_protection_stone");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        // 🌑 ROMPER EL MONOLITO (MAGNETITA)
        if (stone != null && block.getType() == Material.LODESTONE) {
            if (stone.getOwnerId().equals(player.getUniqueId())) {

                // 🌟 HOLOGRAMA: Destruimos el texto flotante antes de borrar la piedra
                stone.removeHologram();

                claimManager.removeStoneFromCache(stone);

                CompletableFuture.runAsync(() -> {
                    try (Connection conn = core.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });

                CrossplayUtils.sendMessage(player, "&#CC66FF[✓] <bold>RITUAL DESHECHO:</bold> &#E6CCFFEl Monolito del Vacío ha sido desmantelado con éxito.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
                return;
            } else {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, "&#FF3366[!] Herejía: &#E6CCFFSolo el Señor de este Dominio puede destruir el Monolito.");
                return;
            }
        }

        // 🌑 ROMPER BLOQUES NORMALES EN TERRITORIO AJENO
        if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF3366[!] Dominio Sellado: &#E6CCFFEl vacío protege estas tierras. No puedes alterar su forma.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());
        if (existingStone != null && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF3366[!] Dominio Sellado: &#E6CCFFNo puedes invocar estructuras en tierras ajenas.");
            return;
        }

        ItemStack itemInHand = event.getItemInHand();

        // 🌑 INVOCAR UN NUEVO MONOLITO
        if (block.getType() == Material.LODESTONE && itemInHand.hasItemMeta()) {

            if (!itemInHand.getItemMeta().getPersistentDataContainer().has(isProtectionStoneKey, PersistentDataType.BYTE)) {
                return;
            }

            Location loc = block.getLocation();
            int radius = limitManager.getProtectionRadius(player);
            UUID newStoneId = UUID.randomUUID();
            NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
            UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

            ClaimBox newBox = new ClaimBox(loc.getWorld().getName(), loc.getBlockX()-radius, -64, loc.getBlockZ()-radius, loc.getBlockX()+radius, 320, loc.getBlockZ()+radius);

            if (claimManager.hasOverlappingClaim(newBox)) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, "&#FF3366[!] Energía Corrupta: &#E6CCFFEl aura de este Monolito colisiona con otro sello cercano. Aléjate más.");
                return;
            }

            ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);
            claimManager.addStoneToCache(newStone);

            // 🌟 HOLOGRAMA: Invocamos el texto flotante
            newStone.updateHologram();

            CrossplayUtils.sendActionBar(player, "&#9933FF[⟳] Conectando con el Abismo...");

            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                if (!canPlace) {
                    Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                        block.setType(Material.AIR);

                        // Si falla el límite, borramos el holograma que acabamos de crear
                        newStone.removeHologram();

                        claimManager.removeStoneFromCache(newStone);

                        ItemStack refundItem = itemInHand.clone();
                        refundItem.setAmount(1);
                        player.getInventory().addItem(refundItem);

                        CrossplayUtils.sendMessage(player, "&#FF3366[!] Límite Alcanzado: &#E6CCFFTu alma no soporta mantener más Monolitos.");
                    });
                    return;
                }

                CrossplayUtils.sendMessage(player, "&#CC66FF[✓] <bold>SELLO INVOCADO:</bold> &#E6CCFFEl Vacío ahora reclama un radio de &#9933FF" + radius + " bloques&#E6CCFF.");
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

        // 🌑 ABRIR EL MENÚ DEL MONOLITO
        if (block.getType() == Material.LODESTONE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null) {
                event.setCancelled(true);

                if (stone.getOwnerId().equals(player.getUniqueId()) || stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) {
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                } else {
                    CrossplayUtils.sendMessage(player, "&#FF3366[!] Herejía: &#E6CCFFEl Monolito rechaza tu tacto.");
                }
            }
        }
    }

    // ==========================================================
    // 🚧 SISTEMA DE FRONTERAS Y CONTROL DE ACCESO (ENTRY)
    // ==========================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        // 🌟 OPTIMIZACIÓN: Solo calculamos si el jugador cruzó a un bloque nuevo
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Consultar el Cerebro: ¿De qué bloque a qué bloque se movió?
        ProtectionStone fromClaim = claimManager.getStoneAt(event.getFrom());
        ProtectionStone toClaim = claimManager.getStoneAt(event.getTo());

        // Si cambió de dominio territorial (Ya sea entrando, saliendo, o cruzando entre dos dueños distintos)
        if (fromClaim != toClaim) {

            // 1. LÓGICA DE ENTRADA AL NUEVO TERRITORIO
            if (toClaim != null) {

                // 🌟 CORRECCIÓN: Se cambió hasFlag por getFlag para coincidir con la arquitectura
                if (!toClaim.getFlag("ENTRY") && !toClaim.getOwnerId().equals(player.getUniqueId()) && !toClaim.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) && !player.hasPermission("nexoprotections.admin")) {
                    CrossplayUtils.sendMessage(player, "&#ff4b2b[!] Campo de Fuerza: &#e0e0e0Este dominio está cerrado para extraños.");

                    // Empujarlo suavemente hacia atrás (Bedrock Friendly)
                    org.bukkit.util.Vector pushback = event.getFrom().toVector().subtract(event.getTo().toVector()).normalize().multiply(0.5);
                    pushback.setY(0.1);
                    player.setVelocity(pushback);

                    event.setCancelled(true);
                    return;
                }

                // Título en la ActionBar informando a qué imperio ha entrado
                String ownerName = org.bukkit.Bukkit.getOfflinePlayer(toClaim.getOwnerId()).getName();
                if (ownerName == null) ownerName = "Desconocido";
                CrossplayUtils.sendActionBar(player, "&#a8ff78🌿 Has entrado al dominio de " + ownerName);
            }

            // 2. LÓGICA DE SALIDA A ZONA SALVAJE
            if (fromClaim != null && toClaim == null) {
                CrossplayUtils.sendActionBar(player, "&#ff4b2b🌲 Has entrado a Zona Salvaje");
            }
        }
    }
}