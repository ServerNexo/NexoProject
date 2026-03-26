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

        // 🌟 Llave maestra del sistema
        this.isProtectionStoneKey = new NamespacedKey(NexoProtections.getPlugin(NexoProtections.class), "is_protection_stone");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        // 🌟 Actualizado a LODESTONE (Magnetita)
        if (stone != null && block.getType() == Material.LODESTONE) {
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
            } else {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Infracción de Seguridad: &#AAAAAASolo el propietario puede desmantelar este Nexo.");
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
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());
        if (existingStone != null && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD)) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Acceso Denegado: &#AAAAAANo puedes construir estructuras en el sector de otra entidad.");
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        // 🌟 Actualizado a LODESTONE
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

            // 🌟 NUEVO: ESCANEO DE COLISIONES
            // Si el radar detecta que esta caja choca con otra que ya está guardada en el ClaimManager
            if (claimManager.hasOverlappingClaim(newBox)) {
                event.setCancelled(true); // Cancelamos para que no se coloque ni te gaste el ítem
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Error Crítico: &#AAAAAAEl campo electromagnético de tu Nexo colisiona con otra zona corporativa cercana.");
                return;
            }

            ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);
            claimManager.addStoneToCache(newStone);

            CrossplayUtils.sendActionBar(player, "&#AAAAAA[⟳] Sincronizando escudo con la red central...");

            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                if (!canPlace) {
                    Bukkit.getScheduler().runTask(NexoProtections.getPlugin(NexoProtections.class), () -> {
                        block.setType(Material.AIR);
                        claimManager.removeStoneFromCache(newStone);

                        ItemStack refundItem = itemInHand.clone();
                        refundItem.setAmount(1);
                        player.getInventory().addItem(refundItem);

                        CrossplayUtils.sendMessage(player, "&#FF5555[!] Cuota Excedida: &#AAAAAAHas alcanzado el límite máximo de protecciones permitidas.");
                    });
                    return;
                }

                CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>ESCUDO OPERATIVO DESPLEGADO:</bold> &#AAAAAASellando un radio de &#00E5FF" + radius + " bloques&#AAAAAA a la red.");

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

        // 🌟 Actualizado a LODESTONE. ¡Al no tener interfaz, el clic se registra instantáneo!
        if (block.getType() == Material.LODESTONE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null) {
                event.setCancelled(true); // Evita que ponga un bloque si tiene uno en la mano

                if (stone.getOwnerId().equals(player.getUniqueId()) || stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT)) {
                    // ¡ABRIMOS EL MENÚ DIRECTAMENTE SIN DELAYS!
                    me.nexo.protections.menu.ProtectionMenu.openMenu(player, stone);
                } else {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Autorización Denegada: &#AAAAAANo posees las credenciales para administrar este Nexo.");
                }
                return;
            }
        }

        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());
        if (stone != null) {
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