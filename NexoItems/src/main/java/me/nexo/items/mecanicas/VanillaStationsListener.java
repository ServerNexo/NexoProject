package me.nexo.items.mecanicas;

import me.nexo.items.NexoItems;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class VanillaStationsListener implements Listener {

    private final NexoItems plugin;
    private final String nexoNamespace;

    public VanillaStationsListener(NexoItems plugin) {
        this.plugin = plugin;
        // Obtenemos el "apellido" de tu plugin en minúsculas (nexoitems o nexo)
        this.nexoNamespace = plugin.getName().toLowerCase();
    }

    // 🔨 1. Bloquear Yunques Vanilla
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0);
        ItemStack slot2 = event.getInventory().getItem(1);

        if (esItemCustom(slot1) || esItemCustom(slot2)) {
            event.setResult(null); // Borra el ítem de resultado visualmente
        }
    }

    // 🪨 2. Bloquear Piedras de Afilar (Grindstones)
    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0);
        ItemStack slot2 = event.getInventory().getItem(1);

        if (esItemCustom(slot1) || esItemCustom(slot2)) {
            event.setResult(null);
        }
    }

    // ✨ 3. Bloquear Mesas de Encantamiento
    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (esItemCustom(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // 🛡️ 4. Bloquear Mesa de Herrería (Smithing Table - Actualizaciones de Netherite)
    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        ItemStack equipo = event.getInventory().getItem(1); // El slot central del equipo
        ItemStack plantilla = event.getInventory().getItem(0);

        if (esItemCustom(equipo) || esItemCustom(plantilla)) {
            event.setResult(null);
        }
    }

    // 🧠 MAGIA SENIOR: Detecta cualquier ítem de tu ecosistema sin importar qué sea
    private boolean esItemCustom(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Escaneamos todas las llaves (keys) de datos ocultos del ítem
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            // Si el ítem tiene un dato registrado por "nexoitems", "nexo" o "nexo_core", lo bloqueamos
            if (key.getNamespace().contains("nexo")) {
                return true;
            }
        }
        return false;
    }
}