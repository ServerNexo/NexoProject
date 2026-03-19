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
            // Generamos el modelo visual de Nexo
            var nexoItemBuilder = NexoItems.itemFromId(type.getNexoModelID());
            if (nexoItemBuilder != null) display.setItemStack(nexoItemBuilder.build());

            display.setBillboard(ItemDisplay.Billboard.FIXED);
            display.setInvulnerable(true);

            // 🐛 Parche: 5 segundos de espera inicial para no escupir al instante
            long tiempoPrimeraAccion = System.currentTimeMillis() + 5000L;

            var pdc = display.getPersistentDataContainer();
            pdc.set(MinionKeys.OWNER, PersistentDataType.STRING, ownerId.toString());
            pdc.set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
            pdc.set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);
            pdc.set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, tiempoPrimeraAccion);
            pdc.set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0); // 📦 Nace con mochila vacía

            Interaction hitbox = loc.getWorld().spawn(loc, Interaction.class, inter -> {
                inter.setInteractionWidth(1.2f);
                inter.setInteractionHeight(1.5f);
                inter.getPersistentDataContainer().set(MinionKeys.OWNER, PersistentDataType.STRING, ownerId.toString());
                inter.getPersistentDataContainer().set(new NamespacedKey(plugin, "minion_display_id"), PersistentDataType.STRING, display.getUniqueId().toString());
            });

            // 🌟 Conectamos la abeja pasándole el 'plugin' para que no dé errores
            minionsActivos.put(display.getUniqueId(), new ActiveMinion(plugin, display, hitbox, ownerId, type, tier, tiempoPrimeraAccion, 0));
        });
    }

    public void recogerMinion(Player player, UUID displayId) {
        ActiveMinion minion = minionsActivos.remove(displayId);
        if (minion != null) {

            // 🌟 PARCHE CRÍTICO: Devolver las Mejoras y Combustibles al recoger el minion
            for (ItemStack upgrade : minion.getUpgrades()) {
                if (upgrade != null && !upgrade.getType().isAir()) {
                    // Se las damos en el inventario o las tiramos al suelo si está lleno
                    var sobrante = player.getInventory().addItem(upgrade);
                    for (ItemStack drop : sobrante.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }

            if (minion.getEntity() != null) minion.getEntity().remove();
            if (minion.getHitbox() != null) minion.getHitbox().remove();

            player.sendMessage("§a§l¡BZZZ! §eHas recogido tu Minion de vuelta.");

            // Avisamos si perdió bloques por no sacarlos del menú antes
            if (minion.getStoredItems() > 0) {
                player.sendMessage("§c⚠️ Se perdieron " + minion.getStoredItems() + " bloques porque no los extraíste del menú.");
            }

            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "minion give " + player.getName() + " " + minion.getType().name() + " " + minion.getTier());
        }
    }

    public void tickAll(long currentTimeMillis) {
        for (ActiveMinion minion : minionsActivos.values()) {
            if (minion.getEntity().isDead() || !minion.getEntity().isValid()) {
                if (minion.getHitbox() != null) minion.getHitbox().remove();
                minionsActivos.remove(minion.getEntity().getUniqueId());
                continue;
            }
            minion.tick(currentTimeMillis);
        }
    }

    // Usado por el Menú GUI para encontrar la abeja correcta
    public ActiveMinion getMinion(UUID displayId) {
        return minionsActivos.get(displayId);
    }

    // Usado por el MinionLoadListener para reconectar abejas al reiniciar
    public ConcurrentHashMap<UUID, ActiveMinion> getMinionsActivos() {
        return minionsActivos;
    }
}