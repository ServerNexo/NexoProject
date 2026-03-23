package me.nexo.protections.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.LimitManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final LimitManager limitManager;
    private final NexoCore core;

    public ProtectionListener(ClaimManager claimManager, LimitManager limitManager) {
        this.claimManager = claimManager;
        this.limitManager = limitManager;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        // 🌟 ¿Está intentando romper el bloque central (El Faro / Nexo)?
        if (stone != null && block.getType() == Material.BEACON) {
            // Solo el DUEÑO de la piedra puede desmantelarla
            if (stone.getOwnerId().equals(player.getUniqueId())) {
                claimManager.removeStoneFromCache(stone); // Borrar de la RAM

                // Borrar de Supabase Asíncronamente
                CompletableFuture.runAsync(() -> {
                    try (Connection conn = core.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });

                player.sendMessage("§aHas desmantelado tu Protección exitosamente.");
                // Opcional: Podrías dropear el ítem del Nexo aquí para que lo recupere.
                return;
            }
        }

        // Si intenta romper un bloque normal dentro de la zona protegida
        if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK)) {
            event.setCancelled(true);
            player.sendMessage("§cNo tienes permiso para romper bloques aquí.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());
        if (existingStone != null && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
            event.setCancelled(true);
            player.sendMessage("§cNo puedes construir en territorio ajeno.");
            return;
        }

        // 🌟 ¿Está colocando una NUEVA Piedra (Faro de Nexo)?
        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand.getType() == Material.BEACON && itemInHand.hasItemMeta()) {

            // Verificamos de forma segura que sea nuestro ítem especial y no un faro normal
            String displayName = itemInHand.getItemMeta().getDisplayName();
            if (displayName == null || !displayName.contains("Nexo de Protección")) {
                return; // Es un faro normal, dejamos que lo coloque sin crear protección
            }

            // Clonamos el ítem por si tenemos que devolvérselo
            ItemStack refundItem = itemInHand.clone();
            refundItem.setAmount(1);

            // Consultamos la BD asíncronamente para ver si no ha superado el límite
            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                if (!canPlace) {
                    // Si alcanzó el límite, cancelamos el bloque en el hilo principal de Bukkit
                    Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                        block.setType(Material.AIR);
                        player.getInventory().addItem(refundItem);
                        player.sendMessage("§cHas alcanzado el límite máximo de protecciones.");
                    });
                    return;
                }

                // Generamos la nueva piedra
                int radius = limitManager.getProtectionRadius(player);
                UUID newStoneId = UUID.randomUUID();
                NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
                UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

                // El ClaimBox va desde el subsuelo hasta el límite del cielo
                ClaimBox newBox = new ClaimBox(block.getWorld().getName(), block.getX()-radius, -64, block.getZ()-radius, block.getX()+radius, 320, block.getZ()+radius);
                ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);

                // Insertamos en la RAM en el hilo principal
                Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                    claimManager.addStoneToCache(newStone);
                    player.sendMessage("§a¡Nexo de Protección establecido! Protegiendo un radio de " + radius + " bloques.");
                });

                // Guardamos en Supabase en el hilo secundario
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
        Player player = event.getPlayer();

        if (stone != null) {
            // 🌟 NUEVO: ¿Le dio Shift + Clic Derecho a la piedra central (Faro)?
            if (block.getType() == Material.BEACON && player.isSneaking() && event.getAction().isRightClick()) {
                event.setCancelled(true);

                // Solo el dueño o un miembro del clan con permisos puede abrir el menú
                if (stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) {
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                } else {
                    player.sendMessage("§cNo tienes permiso para administrar esta piedra.");
                }
                return;
            }

            // Lógica normal de proteger cofres y puertas
            String typeName = block.getType().name();
            ClaimAction action = (typeName.contains("CHEST") || typeName.contains("BARREL") || typeName.contains("SHULKER"))
                    ? ClaimAction.OPEN_CONTAINER : ClaimAction.INTERACT;

            if (!stone.hasPermission(player.getUniqueId(), action)) {
                event.setCancelled(true);
                player.sendMessage("§cEsta zona está protegida.");
            }
        }
    }
}