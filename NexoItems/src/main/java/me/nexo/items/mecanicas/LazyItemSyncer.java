package me.nexo.items.mecanicas;

import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager; // 🌟 AÑADIDO PARA LEER LAS LLAVES
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LazyItemSyncer implements Listener {

    private final NexoItems plugin;

    // 🌟 Claves de datos que DEBEMOS proteger durante la sincronización
    private final NamespacedKey reforgeKey;
    private final NamespacedKey enchantKey;
    private final NamespacedKey prestigeKey;

    public LazyItemSyncer(NexoItems plugin) {
        this.plugin = plugin;
        this.reforgeKey = ItemManager.llaveReforja; // Leemos la llave directamente del Manager
        this.enchantKey = ItemManager.llaveEnchantId;
        this.prestigeKey = ItemManager.llaveNivelEvolucion;
    }

    /**
     * 🛡️ Escaneo Diferido (Lazy Sync):
     * Ocurre solo cuando el jugador interactúa con un contenedor, evitando
     * escanear todos los ítems del servidor en cada tick (Zero-Lag).
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onContainerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // Escaneamos el inventario superior (Cofre, Mochila, etc.)
        sincronizarInventario(event.getInventory());

        // Escaneamos el inventario del propio jugador
        sincronizarInventario(event.getPlayer().getInventory());
    }

    private void sincronizarInventario(Inventory inventory) {
        // Usamos un Virtual Thread para procesar el inventario sin congelar el servidor
        Thread.startVirtualThread(() -> {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.hasItemMeta()) {
                    sincronizarItem(item);
                }
            }
        });
    }

    private void sincronizarItem(ItemStack oldItem) {
        ItemMeta oldMeta = oldItem.getItemMeta();
        PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();

        ItemStack freshTemplate = null;

        // 1. Verificamos qué tipo de ítem Nexo es y generamos el "Molde" fresco
        if (oldPdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
            String id = oldPdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            freshTemplate = ItemManager.generarArmaRPG(id);
        }
        else if (oldPdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING)) {
            String id = oldPdc.get(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
            freshTemplate = ItemManager.generarHerramientaProfesion(id);
        }
        else if (oldPdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
            String id = oldPdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING);
            // Averiguamos qué pieza de armadura es basada en el material original
            String tipoPieza = oldItem.getType().name().split("_")[1]; // Ej: IRON_CHESTPLATE -> CHESTPLATE
            freshTemplate = ItemManager.generarArmaduraProfesion(id, tipoPieza);
        }

        // Si no es un ítem Nexo o fue eliminado de la config, no hacemos nada.
        if (freshTemplate == null) return;

        ItemMeta freshMeta = freshTemplate.getItemMeta();
        PersistentDataContainer freshPdc = freshMeta.getPersistentDataContainer();

        // 2. 🛡️ PROTOCOLO DE PROTECCIÓN DE DATOS:
        // Copiamos los datos valiosos del jugador (Reforjas, Encantamientos, Nivel Cénit) al nuevo meta.

        if (oldPdc.has(reforgeKey, PersistentDataType.STRING)) {
            freshPdc.set(reforgeKey, PersistentDataType.STRING, oldPdc.get(reforgeKey, PersistentDataType.STRING));
        }

        if (oldPdc.has(enchantKey, PersistentDataType.STRING)) {
            freshPdc.set(enchantKey, PersistentDataType.STRING, oldPdc.get(enchantKey, PersistentDataType.STRING));
            if (oldPdc.has(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER)) {
                freshPdc.set(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER, oldPdc.get(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER));
            }
        }

        if (oldPdc.has(prestigeKey, PersistentDataType.INTEGER)) {
            int level = oldPdc.get(prestigeKey, PersistentDataType.INTEGER);
            // Cap estricto de Economía: Máximo Nivel 60
            freshPdc.set(prestigeKey, PersistentDataType.INTEGER, Math.min(level, 60));
        }

        // 3. Aplicamos el nuevo Meta (con el Lore y Nombre actualizados) al ítem original
        // Esto se hace de forma síncrona mediante el scheduler para evitar errores concurrentes de Bukkit
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            oldItem.setItemMeta(freshMeta);
            // Actualizamos visualmente el nivel y la reforja
            ItemManager.sincronizarItemAsync(oldItem);
        });
    }
}