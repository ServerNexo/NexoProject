package me.nexo.minions.listeners;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.menu.MinionMenu;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {
    private final NexoMinions plugin;

    public MenuListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinionMenu menu)) return;

        // Permitimos mover ítems en el propio inventario del jugador
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            return;
        }

        int slot = event.getRawSlot();

        // Verificamos si hizo clic en un hueco de mejora
        boolean esSlotDeMejora = false;
        for (int s : MinionMenu.UPGRADE_SLOTS) {
            if (slot == s) esSlotDeMejora = true;
        }

        if (!esSlotDeMejora) {
            event.setCancelled(true);
        }

        Player player = (Player) event.getWhoClicked();
        ActiveMinion minion = menu.getMinion();

        // 📦 Clic en Extraer Materiales (Slot 31)
        if (slot == 31) {
            int cantidad = minion.getStoredItems();
            if (cantidad <= 0) {
                player.sendMessage(ChatColor.RED + "La mochila está vacía.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.getInventory().addItem(new ItemStack(minion.getType().getTargetMaterial(), cantidad));
            minion.setStoredItems(0);
            minion.getEntity().getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);
            player.sendMessage(ChatColor.GREEN + "¡Extraíste " + cantidad + " ítems!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            player.closeInventory();
        }

        // 🧨 Clic en Recoger Minion (Slot 35)
        if (slot == 35) {
            player.closeInventory();
            plugin.getMinionManager().recogerMinion(player, minion.getEntity().getUniqueId());
        }

        // ⬆️ Clic en Evolucionar (Slot 22)
        if (slot == 22) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) return;

            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo == null) {
                player.sendMessage(ChatColor.RED + "El costo no está configurado.");
                return;
            }

            int reqCant = costo.getInt("cantidad");
            String reqMat = costo.getString("material", "");
            String reqNexo = costo.getString("nexo_id", "");

            boolean pagoExitoso = cobrarItems(player, reqCant, reqMat, reqNexo);

            if (!pagoExitoso) {
                player.sendMessage(ChatColor.RED + "No tienes los materiales necesarios para evolucionar.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 🌟 ¡EVOLUCIÓN EXITOSA!
            minion.setTier(sigNivel);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendMessage(ChatColor.GREEN + "¡Tu Minion ha evolucionado al Nivel " + sigNivel + "!");
            minion.getEntity().getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, minion.getEntity().getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            // Recargamos el menú
            player.openInventory(new MinionMenu(plugin, minion).getInventory());
        }
    }

    // 🌟 Helper para cobrar ítems (Soporta Vanilla y Nexo)
    private boolean cobrarItems(Player player, int cantidadNecesaria, String material, String nexoId) {
        int recolectado = 0;
        NamespacedKey nexoKey = new NamespacedKey("nexo", "id");

        // Primero verificamos si tiene suficientes
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }

            if (coincide) recolectado += item.getAmount();
        }

        if (recolectado < cantidadNecesaria) return false;

        // Si tiene suficientes, procedemos a cobrarlos
        int porCobrar = cantidadNecesaria;
        for (ItemStack item : player.getInventory().getContents()) {
            if (porCobrar <= 0) break;
            if (item == null || item.getType().isAir()) continue;

            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }

            if (coincide) {
                if (item.getAmount() <= porCobrar) {
                    porCobrar -= item.getAmount();
                    item.setAmount(0); // Borra el stack
                } else {
                    item.setAmount(item.getAmount() - porCobrar);
                    porCobrar = 0;
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinionMenu menu)) return;

        ActiveMinion minion = menu.getMinion();
        for (int i = 0; i < 4; i++) {
            int invSlot = MinionMenu.UPGRADE_SLOTS[i];
            ItemStack itemEnSlot = event.getInventory().getItem(invSlot);
            minion.setUpgrade(i, itemEnSlot);
        }
    }
}