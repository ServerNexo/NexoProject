package me.nexo.dungeons.listeners;

import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class LootProtectionListener implements Listener {

    private final NexoDungeons plugin;
    public static NamespacedKey ownerKey;

    public LootProtectionListener(NexoDungeons plugin) {
        this.plugin = plugin;
        // Creamos la llave NBT para marcar a los dueños
        ownerKey = new NamespacedKey(plugin, "loot_owner");
    }

    // =========================================
    // 🎁 UTILIDAD: Soltar Botín Estilo Hypixel
    // =========================================
    public static void dropProtectedItem(Location loc, ItemStack itemStack, Player owner) {
        if (itemStack == null || itemStack.getType().isAir()) return;

        // Soltamos el ítem en el mundo
        Item itemEntity = loc.getWorld().dropItemNaturally(loc, itemStack);

        // Lo hacemos brillar para que se vea épico
        itemEntity.setGlowing(true);

        // Le ponemos un holograma flotante
        String rarezaColor = "§d§l"; // Puedes dinamizar esto según el ítem
        itemEntity.setCustomName(rarezaColor + "Botín de " + owner.getName());
        itemEntity.setCustomNameVisible(true);

        // 🔒 Le inyectamos el UUID del dueño en sus datos persistentes
        itemEntity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
    }

    // =========================================
    // 🛡️ LISTENER: Bloquear a los ladrones
    // =========================================
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        // Solo nos importan los jugadores
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();

        // Verificamos si este ítem está protegido por nuestro sistema
        if (item.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            String ownerUUID = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

            // Si el UUID del jugador NO coincide con el UUID guardado en el ítem...
            if (!player.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true); // Bloqueamos la acción
                // Opcional: Descomentar para avisarle que no es suyo, aunque puede causar spam de chat
                // player.sendMessage("§c¡Ese botín le pertenece a otro jugador!");
            }
        }
    }
}