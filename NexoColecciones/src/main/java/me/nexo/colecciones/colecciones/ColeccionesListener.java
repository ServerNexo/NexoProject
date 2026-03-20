package me.nexo.colecciones.colecciones;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.NexoCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class ColeccionesListener implements Listener {

    private final NexoColecciones plugin;
    private final CollectionManager manager;
    private static final String ANTI_EXPLOIT_KEY = "nexo_player_placed";
    private static final String BREWER_KEY = "nexo_last_brewer";

    public ColeccionesListener(NexoColecciones plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCollectionManager();
    }

    // ==========================================
    // 🛡️ ANTI-EXPLOIT (Marcador)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata(ANTI_EXPLOIT_KEY, new FixedMetadataValue(plugin, true));
    }

    // ==========================================
    // ⛏️ TALA, MINERÍA Y FARMING
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockId = block.getType().name();

        if (block.getBlockData() instanceof Ageable cultivo) {
            if (cultivo.getAge() < cultivo.getMaximumAge()) return;
            manager.addProgress(event.getPlayer(), blockId, 1);
            if (block.hasMetadata(ANTI_EXPLOIT_KEY)) block.removeMetadata(ANTI_EXPLOIT_KEY, plugin);
            return;
        }

        if (block.hasMetadata(ANTI_EXPLOIT_KEY)) {
            block.removeMetadata(ANTI_EXPLOIT_KEY, plugin);
            return;
        }

        manager.addProgress(event.getPlayer(), blockId, 1);
    }

    // ==========================================
    // ⚔️ FIGHTING (MOBS)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            String mobType = event.getEntity().getType().name();
            manager.addProgress(killer, mobType, 1);
        }
    }

    // ==========================================
    // 🎣 FISHING (PESCA)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item itemCaught) {
            String fishType = itemCaught.getItemStack().getType().name();
            manager.addProgress(event.getPlayer(), fishType, 1);
        }
    }

    // ==========================================
    // 🧪 ALQUIMIA (POCIONES)
    // ==========================================
    // 1. Guardamos quién fue el último en abrir el soporte para pociones
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING) {
            if (event.getInventory().getLocation() != null) {
                event.getInventory().getLocation().getBlock().setMetadata(BREWER_KEY, new FixedMetadataValue(plugin, event.getPlayer().getUniqueId().toString()));
            }
        }
    }

    // 2. Cuando la poción termina de hervir, le damos el punto al jugador por el ingrediente que usó
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (event.getBlock().hasMetadata(BREWER_KEY)) {
            String uuidStr = event.getBlock().getMetadata(BREWER_KEY).get(0).asString();
            Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));

            if (player != null && player.isOnline()) {
                ItemStack ingrediente = event.getContents().getIngredient();
                if (ingrediente != null && ingrediente.getType() != Material.AIR) {
                    manager.addProgress(player, ingrediente.getType().name(), 1);
                }
            }
        }
    }

    // ==========================================
    // 📚 ENCANTAMIENTOS
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        // Suma progreso a Lapislázuli (la cantidad exacta que costó el encantamiento)
        int lapisUsado = event.whichButton() + 1; // Botón 0 = 1 lapis, Botón 1 = 2 lapis, Botón 2 = 3 lapis
        manager.addProgress(player, "LAPIS_LAZULI", lapisUsado);

        // Si lo que encantó fue un Libro, le damos puntos a la colección de Libros
        if (event.getItem().getType() == Material.BOOK) {
            manager.addProgress(player, "BOOK", 1);
        }
    }

    // ==========================================
    // 📥 GESTIÓN DE DATOS DEL JUGADOR
    // ==========================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var dataSource = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getDataSource();
        manager.loadPlayerFromDatabase(event.getPlayer().getUniqueId(), dataSource);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removeProfile(event.getPlayer().getUniqueId());
    }
}