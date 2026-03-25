package me.nexo.protections.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils; // 🌟 TRADUCTOR UNIVERSAL
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
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
import org.bukkit.inventory.EquipmentSlot;
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
        // [CÓDIGO INTACTO - Ya estaba perfecto]
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        if (stone != null && block.getType() == Material.BEACON) {
            if (stone.getOwnerId().equals(player.getUniqueId())) {
                claimManager.removeStoneFromCache(stone);

                CompletableFuture.runAsync(() -> {
                    try (Connection conn = core.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });

                CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>PROTOCOLO DE DESMANTELAMIENTO:</bold> &#AAAAAANexo de protección desconectado exitosamente.");
                return;
            }
        }

        if (stone != null && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Infracción de Seguridad: &#AAAAAATerritorio corporativo ajeno. No tienes permisos de minería.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // [CÓDIGO INTACTO - Ya estaba perfecto]
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());
        if (existingStone != null && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Acceso Denegado: &#AAAAAANo puedes construir estructuras en el sector de otra entidad.");
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand.getType() == Material.BEACON && itemInHand.hasItemMeta()) {

            String displayName = itemInHand.getItemMeta().getDisplayName();
            if (displayName == null || !displayName.contains("Nexo de Protección")) return;

            ItemStack refundItem = itemInHand.clone();
            refundItem.setAmount(1);

            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                if (!canPlace) {
                    Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                        block.setType(Material.AIR);
                        player.getInventory().addItem(refundItem);
                        CrossplayUtils.sendMessage(player, "&#FF5555[!] Cuota Excedida: &#AAAAAAHas alcanzado el límite máximo de protecciones permitidas.");
                    });
                    return;
                }

                int radius = limitManager.getProtectionRadius(player);
                UUID newStoneId = UUID.randomUUID();
                NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
                UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

                ClaimBox newBox = new ClaimBox(block.getWorld().getName(), block.getX()-radius, -64, block.getZ()-radius, block.getX()+radius, 320, block.getZ()+radius);
                ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);

                Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                    claimManager.addStoneToCache(newStone);
                    CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>ESCUDO OPERATIVO DESPLEGADO:</bold> &#AAAAAASellando un radio de &#00E5FF" + radius + " bloques&#AAAAAA a la red.");
                });

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
        // 🌟 PROTECCIÓN CROSS-PLAY: Evitamos el bug del doble-click en Bedrock
        // ignorando la interacción de la mano secundaria (OFF_HAND)
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
        Player player = event.getPlayer();

        if (stone != null) {
            // 🌟 MEJORA UX (MOBILE-FRIENDLY): Quitamos el player.isSneaking()
            // Ahora basta con darle un simple clic derecho al Faro para abrir el menú
            if (block.getType() == Material.BEACON && event.getAction().isRightClick()) {
                event.setCancelled(true);

                if (stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) {
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                } else {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Autorización Denegada: &#AAAAAANo posees las credenciales para administrar este Nexo.");
                }
                return; // Cortamos el código aquí para que no evalúe más abajo
            }

            // Lógica normal de proteger cofres y puertas del terreno
            String typeName = block.getType().name();
            ClaimAction action = (typeName.contains("CHEST") || typeName.contains("BARREL") || typeName.contains("SHULKER"))
                    ? ClaimAction.OPEN_CONTAINER : ClaimAction.INTERACT;

            if (!stone.hasPermission(player.getUniqueId(), action)) {
                event.setCancelled(true);
                CrossplayUtils.sendActionBar(player, "&#FF5555[!] Acceso Restringido: Propiedad privada asegurada.");
            }
        }
    }
}