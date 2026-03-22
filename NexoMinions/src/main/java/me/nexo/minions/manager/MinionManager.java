package me.nexo.minions.manager;

import com.nexomc.nexo.api.NexoItems;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MinionManager {

    private final NexoMinions plugin;
    private final ConcurrentHashMap<UUID, ActiveMinion> minionsActivos = new ConcurrentHashMap<>();

    public MinionManager(NexoMinions plugin) {
        this.plugin = plugin;
        MinionKeys.init(plugin);
    }

    public void spawnMinion(Location loc, UUID ownerId, MinionType type, int tier) {
        loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            var nexoItemBuilder = NexoItems.itemFromId(type.getNexoModelID());
            if (nexoItemBuilder != null) display.setItemStack(nexoItemBuilder.build());

            display.setBillboard(ItemDisplay.Billboard.FIXED);
            display.setInvulnerable(true);

            display.setInterpolationDuration(20);
            display.setInterpolationDelay(0);

            long tiempoPrimeraAccion = System.currentTimeMillis() + 5000L;

            var pdc = display.getPersistentDataContainer();
            pdc.set(MinionKeys.OWNER, PersistentDataType.STRING, ownerId.toString());
            pdc.set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
            pdc.set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);
            pdc.set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, tiempoPrimeraAccion);
            pdc.set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            Interaction hitbox = loc.getWorld().spawn(loc, Interaction.class, inter -> {
                inter.setInteractionWidth(1.2f);
                inter.setInteractionHeight(1.5f);
                inter.getPersistentDataContainer().set(MinionKeys.OWNER, PersistentDataType.STRING, ownerId.toString());
                inter.getPersistentDataContainer().set(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING, display.getUniqueId().toString());
            });

            Location holoLoc = loc.clone().add(0, 1.2, 0);
            TextDisplay holograma = loc.getWorld().spawn(holoLoc, TextDisplay.class, holo -> {
                holo.setBillboard(TextDisplay.Billboard.CENTER);
                holo.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
                holo.setText("§eCargando Abeja...");
            });

            pdc.set(new NamespacedKey(plugin, "minion_holo_id"), PersistentDataType.STRING, holograma.getUniqueId().toString());

            minionsActivos.put(display.getUniqueId(), new ActiveMinion(plugin, display, hitbox, holograma, ownerId, type, tier, tiempoPrimeraAccion, 0));
        });
    }

    public void recogerMinion(Player player, UUID displayId) {
        ActiveMinion minion = minionsActivos.remove(displayId);
        if (minion != null) {

            // Devolver las Mejoras y Combustibles al recoger el minion
            for (ItemStack upgrade : minion.getUpgrades()) {
                if (upgrade != null && !upgrade.getType().isAir()) {
                    var sobrante = player.getInventory().addItem(upgrade);
                    for (ItemStack drop : sobrante.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }

            // 🌟 NUEVO: Entregarle al jugador los ítems que el minion tenía en su mochila
            if (minion.getStoredItems() > 0) {
                int cantidad = minion.getStoredItems();
                org.bukkit.Material mat = minion.getType().getTargetMaterial();

                // Dividimos en stacks de 64 para no romper el inventario
                while (cantidad > 0) {
                    int dar = Math.min(cantidad, 64);
                    ItemStack recompensa = new ItemStack(mat, dar);
                    var sobrante = player.getInventory().addItem(recompensa);
                    for (ItemStack drop : sobrante.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    cantidad -= dar;
                }
                player.sendMessage("§a📦 Has recuperado " + minion.getStoredItems() + " ítems que la Abeja tenía guardados.");
            }

            if (minion.getEntity() != null) minion.getEntity().remove();
            if (minion.getHitbox() != null) minion.getHitbox().remove();
            if (minion.getHolograma() != null) minion.getHolograma().remove();

            // 🚨 CORRECCIÓN: Le devolvemos el espacio libre al VERDADERO DUEÑO
            Player owner = org.bukkit.Bukkit.getPlayer(minion.getOwnerId());
            if (owner != null && owner.isOnline()) {
                addPlacedMinion(owner, -1);

                if (owner.getUniqueId().equals(player.getUniqueId())) {
                    owner.sendMessage("§a§l¡BZZZ! §eHas recogido tu Minion. §7(" + getPlacedMinions(owner) + "/" + getMaxMinions(owner) + ")");
                } else {
                    owner.sendMessage("§c⚠️ Un administrador ha recogido uno de tus Minions.");
                    player.sendMessage("§aMinion recogido. El espacio le fue devuelto a su dueño legítimo.");
                }
            } else {
                player.sendMessage("§c⚠️ ADVERTENCIA: El dueño del Minion está OFFLINE. Su límite de minions podría desincronizarse.");
            }

            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "minion give " + player.getName() + " " + minion.getType().name() + " " + minion.getTier());
        }
    }

    public void tickAll(long currentTimeMillis) {
        for (ActiveMinion minion : minionsActivos.values()) {
            if (minion.getEntity().isDead()) {
                if (minion.getHitbox() != null) minion.getHitbox().remove();
                if (minion.getHolograma() != null) minion.getHolograma().remove();
                minionsActivos.remove(minion.getEntity().getUniqueId());
                continue;
            }

            // 🌟 PARCHE DE MEMORIA RAM: Si el chunk se descarga, lo borramos de la RAM.
            // Al volver, el MinionLoadListener lo reconstruirá y calculará el farmeo offline.
            if (!minion.getEntity().isValid()) {
                minionsActivos.remove(minion.getEntity().getUniqueId());
                continue;
            }

            minion.tick(currentTimeMillis);
        }
    }

    public ActiveMinion getMinion(UUID displayId) {
        return minionsActivos.get(displayId);
    }

    // =========================================
    // 🌟 SISTEMA DE LÍMITES POR JUGADOR
    // =========================================
    public int getPlacedMinions(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "minions_placed");
        return player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public void addPlacedMinion(Player player, int amount) {
        NamespacedKey key = new NamespacedKey(plugin, "minions_placed");
        int current = getPlacedMinions(player);
        player.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.max(0, current + amount));
    }

    public int getMaxMinions(Player player) {
        for (int i = 50; i >= 1; i--) {
            if (player.hasPermission("nexominions.limit." + i)) return i;
        }
        return 5; // Límite base para usuarios nuevos
    }

    public ConcurrentHashMap<UUID, ActiveMinion> getMinionsActivos() {
        return minionsActivos;
    }
}